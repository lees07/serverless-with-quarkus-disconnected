# Serverless serving with quarkus native demo in Disconntected Environment

## install Serverless Operator
Find Serverless Operator on OpenShift console -> OpeartorHub, and install it.  

## install Knative serving
OpenShift Serverless is based on Knative, serverless applications will be deployed by Knative, so Knative needs to manage the application images. Knative does manage image registries' CA trust and login it independently, rather than use image registry's CA and pull secret in Kubernetes/OpenShift cluster.  

When install Knative serving, you will create registry's CA certificate as secret.  
```
oc create secret generic registryca --from-file=ca.crt=/etc/pki/ca-trust/source/anchors/domain.crt -n knative-serving
```

Install Serverless serving and use the secret  
```
cat > knative-serving.yaml << EOF
apiVersion: operator.knative.dev/v1beta1
kind: KnativeServing
metadata:
  name: knative-serving
  namespace: knative-serving
spec:
  controller-custom-certs:
    name: registryca
    type: Secret

EOF

oc apply -f knative-serving.yaml
```
> must install into knative-serving namespace which Serverless Operator creates automatically  



## Microsweeper app works as Serverless Serving
1. fork from github and update codes to use quarkus 3.2.6, and support airgap environment  
```
git clone https://github.com/jamesfalkner/microsweeper-quarkus.git
```
> update javax package name to jakarta in codes, java version from 11 to 17 in pom.xml, and store js package in project.

2. create database in postgres of local environment  
```
podman exec -it pgs10 /bin/bash
psql
\l
CREATE DATABASE score;
CREATE USER quarkus WITH ENCRYPTED PASSWORD ********;
GRANT ALL PRIVILEGES ON DATABASE score TO quarkus;
ALTER DATABASE score OWNER TO quarkus;
\l
\q
exit
```

3. build quarkus uber-jar and native application to test  
```
mvn clean package
java -jar microsweeper-appservice-1.0.0-SNAPSHOT-runner.jar

mvn clean package -Dnative
microsweeper-appservice-1.0.0-SNAPSHOT-runner
```
> install java-17, download maven and mandrel, setup MAVEN_HOME, GRAALVM_HOME and PATH.  
> application will open port 8080 to serve, use firewall-cmd to add-port.  
> use same rhel version to build native and run.  

4. build container and push to local registry  
```
cat > Containerfile-jar << EOF 
FROM registry.redhat.io/ubi8/openjdk-17-runtime:1.17

COPY uber-jar/*-runner.jar /deployments/quarkus-run.jar

EXPOSE 8080
USER 185
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"
EOF

podman build . -t registry.ocp4.example.com/demo/microsweeper-jar:v1 -f Containerfile-jar
podman push registry.ocp4.example.com/demo/microsweeper-jar:v1


cat > Containerfile-jar << EOF
FROM registry.redhat.io/ubi8/openjdk-17-runtime:1.17

COPY uber-jar/*-runner.jar /deployments/quarkus-run.jar

EXPOSE 8080
USER 185
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"
[root@helper-ocp4test microsweeper]# cat Containerfile-native 
FROM registry.redhat.io/ubi8/ubi-minimal:8.8
WORKDIR /work/
RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work
COPY --chown=1001:root elf/*-runner /work/application

EXPOSE 8080
USER 1001

CMD ["./application"]
EOF

podman build . -t registry.ocp4.example.com/demo/microsweeper-native:v2 -f Containerfile-native
podman push registry.ocp4.example.com/demo/microsweeper-native:v2
```

5. create knative-serving with some parameter for customization  
create pull secret of local image registry as secret  
```
oc create secret docker-registry quay-registry --docker-server=https://registry.ocp4.example.com/ --docker-username=test1 --docker-password=********* -n test
```

create knative service and multi revisions  
```
kn service create microsweeper --image registry.ocp4.example.com/demo/microsweeper-jar:v1 --port 8080 --limit cpu=100m,memory=512Mi --scale 0..2 --revision-name quarkus --pull-secret quay-registry -n test

oc get ksvc -n test

kn service update microsweeper --image registry.ocp4.example.com/demo/microsweeper-native:v2 --revision-name quarkus-native -n test

kn revision list

oc get pod -n test
```

6. test scale to zero and compare springboot, quarkus and quarkus native start time  
Open Chrome browser, access https://microsweeper-test.apps.ocp4.example.com, try game serveral times.  
You will find that, after your request sent, Knative starts a pod to serve, and kill it after 30 seconds(default configure) later.  
The request won't be lost in pod starting period, but be queued.  

check data in database.  
```
podman exec -it pgs10 /bin/bash
psql -d score
\d
select * from score;
\q
exit
```

compare codes of spring boot and quarkus implementing  
and deployment spring boot version as new revision of ksvc  
```
cat > Containerfile-springboot << EOF 
FROM registry.redhat.io/ubi8/openjdk-17-runtime:1.17

COPY uber-jar/*-exec.jar /deployments/spring-exec.jar

EXPOSE 8080
USER 185
ENV JAVA_APP_JAR="/deployments/spring-exec.jar"
EOF

podman build . -t registry.ocp4.example.com/demo/microsweeper-spring:v1 -f Containerfile-springboot
podman push registry.ocp4.example.com/demo/microsweeper-jar:v1

kn service update microsweeper --image registry.ocp4.example.com/demo/microsweeper-spring:v1 --revision-name springboot -n test

kn revision list
```

You will find that, at limited resources environment, e.g. cpu=100m,memory=512Mi, native application boot time <1s, but spring boot application boot time >120s, quarkus native technology is advanced in Serverless use cases.  
```
kn service update microsweeper --traffic microsweeper-springboot=100
kn revision list
oc logs microsweeper-springboot-deployment-7599cd4c6-????? -n test
Defaulted container "user-container" out of: user-container, queue-proxy
INFO exec -a "java" java -XX:MaxRAMPercentage=80.0 -XX:+UseParallelGC -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:+ExitOnOutOfMemoryError -cp "." -jar /deployments/spring-exec.jar 
INFO running in /deployments

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

2023-12-01T09:41:14.737Z  INFO 1 --- [           main] c.r.d.m.ScoreboardApplication            : Starting ScoreboardApplication using Java 17.0.8 with PID 1 (/deployments/spring-exec.jar started by 1000760000 in /deployments)
2023-12-01T09:41:14.741Z  INFO 1 --- [           main] c.r.d.m.ScoreboardApplication            : No active profile set, falling back to 1 default profile: "default"
2023-12-01T09:41:42.436Z  INFO 1 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2023-12-01T09:41:44.539Z  INFO 1 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 1799 ms. Found 1 JPA repository interface.
2023-12-01T09:42:05.543Z  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port 8080 (http)
2023-12-01T09:42:05.941Z  INFO 1 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2023-12-01T09:42:05.942Z  INFO 1 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.16]
2023-12-01T09:42:10.140Z  INFO 1 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2023-12-01T09:42:10.238Z  INFO 1 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 53197 ms
2023-12-01T09:42:23.637Z  INFO 1 --- [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2023-12-01T09:42:25.139Z  INFO 1 --- [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.3.1.Final
2023-12-01T09:42:26.139Z  INFO 1 --- [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2023-12-01T09:42:32.237Z  INFO 1 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2023-12-01T09:42:33.141Z  INFO 1 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2023-12-01T09:42:37.140Z  INFO 1 --- [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@27896d3b
2023-12-01T09:42:37.142Z  INFO 1 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2023-12-01T09:42:39.737Z  WARN 1 --- [           main] org.hibernate.dialect.Dialect            : HHH000511: The 10.21.0 version for [org.hibernate.dialect.PostgreSQLDialect] is no longer supported, hence certain features may not work properly. The minimum supported version is 11.0.0. Check the community dialects project for available legacy versions.
2023-12-01T09:43:01.937Z  INFO 1 --- [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2023-12-01T09:43:02.443Z  WARN 1 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Warning Code: 0, SQLState: 00000
2023-12-01T09:43:02.537Z  WARN 1 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : table "score" does not exist, skipping
2023-12-01T09:43:02.539Z  WARN 1 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Warning Code: 0, SQLState: 00000
2023-12-01T09:43:02.539Z  WARN 1 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : sequence "score_seq" does not exist, skipping
2023-12-01T09:43:03.437Z  INFO 1 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2023-12-01T09:43:05.843Z  WARN 1 --- [           main] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
2023-12-01T09:43:06.643Z  INFO 1 --- [           main] o.s.b.a.w.s.WelcomePageHandlerMapping    : Adding welcome page: class path resource [META-INF/resources/index.html]
2023-12-01T09:43:26.437Z  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path ''
2023-12-01T09:43:27.237Z  INFO 1 --- [           main] c.r.d.m.ScoreboardApplication            : Started ScoreboardApplication in 147.697 seconds (process running for 163.397)



kn service update microsweeper --traffic microsweeper-quarkus=100
kn revision list
oc logs microsweeper-quarkus-deployment-5fc9884c8d-????? -n test
Defaulted container "user-container" out of: user-container, queue-proxy
INFO exec -a "java" java -XX:MaxRAMPercentage=80.0 -XX:+UseParallelGC -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:+ExitOnOutOfMemoryError -cp "." -jar /deployments/quarkus-run.jar 
INFO running in /deployments
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2023-12-01 09:53:27,940 WARN  [io.qua.net.run.NettyRecorder] (Thread-0) Netty DefaultChannelId initialization (with io.netty.machineId system property set to 5f:2f:a8:b4:1a:f5:f7:1f) took more than a second
2023-12-01 09:53:59,938 INFO  [io.quarkus] (main) microsweeper-appservice 1.0.0-SNAPSHOT on JVM (powered by Quarkus 3.2.6.Final) started in 58.100s. Listening on: http://0.0.0.0:8080
2023-12-01 09:53:59,938 INFO  [io.quarkus] (main) Profile prod activated. 
2023-12-01 09:53:59,938 INFO  [io.quarkus] (main) Installed features: [agroal, cdi, hibernate-orm, hibernate-orm-panache, jdbc-h2, jdbc-postgresql, micrometer, narayana-jta, resteasy-reactive, resteasy-reactive-jsonb, smallrye-context-propagation, vertx]




kn service update microsweeper --traffic microsweeper-quarkus-native=100
kn revision list
oc logs microsweeper-quarkus-native-deployment-68b45bd974-????? -n test
Defaulted container "user-container" out of: user-container, queue-proxy
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2023-12-01 07:44:33,542 WARN  [org.hib.eng.jdb.spi.SqlExceptionHelper] (JPA Startup Thread) SQL Warning Code: 0, SQLState: 00000
2023-12-01 07:44:33,543 WARN  [org.hib.eng.jdb.spi.SqlExceptionHelper] (JPA Startup Thread) table "score" does not exist, skipping
2023-12-01 07:44:33,543 WARN  [org.hib.eng.jdb.spi.SqlExceptionHelper] (JPA Startup Thread) SQL Warning Code: 0, SQLState: 00000
2023-12-01 07:44:33,543 WARN  [org.hib.eng.jdb.spi.SqlExceptionHelper] (JPA Startup Thread) sequence "score_seq" does not exist, skipping
2023-12-01 07:44:33,637 INFO  [io.quarkus] (main) microsweeper-appservice 1.0.0-SNAPSHOT native (powered by Quarkus 3.2.6.Final) started in 0.500s. Listening on: http://0.0.0.0:8080
2023-12-01 07:44:33,637 INFO  [io.quarkus] (main) Profile prod activated. 
2023-12-01 07:44:33,637 INFO  [io.quarkus] (main) Installed features: [agroal, cdi, hibernate-orm, hibernate-orm-panache, jdbc-h2, jdbc-postgresql, micrometer, narayana-jta, resteasy-reactive, resteasy-reactive-jsonb, smallrye-context-propagation, vertx]
```

If you like to use .Net to develop applications, please reference:  
> .Net apps on rhel reference: https://access.redhat.com/documentation/en-us/net/7.0/html-single/getting_started_with_.net_on_rhel_8/index
> .Net native apps reference: https://learn.microsoft.com/en-us/dotnet/core/deploying/native-aot/?tabs=net7%2Cwindows


## Reference
[Install Serverless Operator](https://docs.openshift.com/serverless/1.30/install/install-serverless-operator.html)
[Install Knative serving](https://docs.openshift.com/serverless/1.30/install/installing-knative-serving.html)
[Create serverless application](https://docs.openshift.com/serverless/1.30/knative-serving/getting-started/serverless-applications.html)
[Quarkus Development](https://quarkus.io/)
[Quarkus Native](https://quarkus.io/guides/building-native-image)
[Mandrel for Quarkus](https://github.com/graalvm/mandrel/releases)

> problem: can't delete latest revision of ksvc by "kn revision delete <revision name>", why?  


