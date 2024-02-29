# Serverless func demo in Disconntected Environment

This func demo follows knative serving's. Please reference [Serverless serving with quarkus native demo in Disconntected Environment](./knative-serving-with-quarkus-native.md) for details first.

## Create a knative func framework
In development environment, "kn", "podman", "buildah" and "skopeo" tools are needed.  
A knative func framework can be created by "kn func create" command line:  
```
kn version
Version:      v1.9.2
Build Date:   2023-09-27 05:43:12
Git Revision: 667dab79
Supported APIs:
* Serving
  - serving.knative.dev/v1 (knative-serving v1.9.0)
* Eventing
  - sources.knative.dev/v1 (knative-eventing v1.9.0)
  - eventing.knative.dev/v1 (knative-eventing v1.9.0)

kn func create quarkus-func-demo -l quarkus -t http

tree
.
├── func.yaml
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
└── src
    ├── main
    │   ├── java
    │   │   └── functions
    │   │       ├── Function.java
    │   │       ├── Input.java
    │   │       └── Output.java
    │   └── resources
    │       └── application.properties
    └── test
        └── java
            └── functions
                ├── FunctionTest.java
                └── NativeFunctionIT.java
```
As example command above, the func's develop language is "quarkus" and message type is "http". A directory named quarkus-func-test created, it includes quarkus java codes and configure files. Please reference "kn func create --help" for details.  

You will implement func function and follow README.md file to enable native compile for func.  
```
cat func.yaml 
specVersion: 0.35.0
name: quarkus-func-test
runtime: quarkus
registry: ""
image: ""
created: 2024-02-26T17:01:38.548983305+08:00
build:
  buildEnvs:
  - name: BP_JVM_VERSION
    value: "17"
  - name: BP_NATIVE_IMAGE
    value: "true"
  - name: BP_MAVEN_BUILT_ARTIFACT
    value: func.yaml target/native-sources/*
  - name: BP_MAVEN_BUILD_ARGUMENTS
    value: package -DskipTests=true -Dmaven.javadoc.skip=true -Dquarkus.package.type=native-sources
  - name: BP_NATIVE_IMAGE_BUILD_ARGUMENTS_FILE
    value: native-image.args
  - name: BP_NATIVE_IMAGE_BUILT_ARTIFACT
    value: '*-runner.jar'
  pvcSize: 256Mi
```

## Build the knative func
If the development environment can access internet, then:  
```
kn func build -p quarkus-func-test -r registry.ocp4.example.com/demo -v
Building function image
STEP 1/9: FROM registry.access.redhat.com/ubi8/openjdk-17
Trying to pull registry.access.redhat.com/ubi8/openjdk-17:latest...
...
STEP 2/9: LABEL "io.k8s.display-name"="registry.ocp4.example.com/demo/quarkus-func-test:latest"       "io.openshift.s2i.build.image"="registry.access.redhat.com/ubi8/openjdk-17"       "io.openshift.s2i.build.source-location"="quarkus-func-test"       "io.openshift.s2i.scripts-url"="image:///usr/local/s2i"
...
STEP 3/9: ENV BP_NATIVE_IMAGE_BUILD_ARGUMENTS_FILE="native-image.args"     BP_NATIVE_IMAGE_BUILT_ARTIFACT="*-runner.jar"     BP_JVM_VERSION="17"     BP_NATIVE_IMAGE="true"     BP_MAVEN_BUILT_ARTIFACT="func.yaml target/native-sources/*"     BP_MAVEN_BUILD_ARGUMENTS="package -DskipTests=true -Dmaven.javadoc.skip=true -Dquarkus.package.type=native-sources"
...
STEP 4/9: USER root
...
STEP 5/9: COPY upload/src /tmp/src
...
STEP 6/9: RUN chown -R 1001:0 /tmp/src
...
STEP 7/9: USER 1001
...
STEP 8/9: RUN /usr/local/s2i/assemble
INFO Performing Maven build in /tmp/src
INFO Using MAVEN_OPTS -XX:MaxRAMPercentage=80.0 -XX:+UseParallelGC -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:+ExitOnOutOfMemoryError -XX:MaxRAMPercentage=25.0
INFO Using Apache Maven 3.8.5 (Red Hat 3.8.5-4)
Maven home: /usr/share/maven
Java version: 17.0.10, vendor: Red Hat, Inc., runtime: /usr/lib/jvm/java-17-openjdk-17.0.10.0.7-2.el8.x86_64
Default locale: en, platform encoding: UTF-8
OS name: "linux", version: "4.18.0-372.9.1.el8.x86_64", arch: "amd64", family: "unix"
INFO Running 'mvn -e -Popenshift -DskipTests -Dcom.redhat.xpaas.repo.redhatga -Dfabric8.skip=true -Djkube.skip=true --batch-mode -Djava.net.preferIPv4Stack=true -s /tmp/artifacts/configuration/settings.xml -Dmaven.repo.local=/tmp/artifacts/m2  package'
[INFO] Error stacktraces are turned on.
[INFO] Scanning for projects...
[INFO] Downloading from central: https://repo1.maven.org/maven2/com/redhat/quarkus/platform/quarkus-maven-plugin/2.13.8.SP2-redhat-00001/quarkus-maven-plugin-2.13.8.SP2-redhat-00001.pom
...
[INFO] Building jar: /tmp/src/target/function-1.0.0-SNAPSHOT.jar
[INFO] 
[INFO] --- quarkus-maven-plugin:2.13.8.SP2-redhat-00001:build (default) @ function ---
[INFO] [io.quarkus.deployment.QuarkusAugmentor] Quarkus augmentation completed in 2387ms
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  14:57 min
[INFO] Finished at: 2024-02-27T08:36:03Z
[INFO] ------------------------------------------------------------------------
...
STEP 9/9: CMD /usr/local/s2i/run
COMMIT registry.ocp4.example.com/demo/quarkus-func-test:latest
...
Successfully tagged registry.ocp4.example.com/demo/quarkus-func-test:latest
a7a858cbf4ddd3649e2747f7f84ed73b8cab5ea279db78f36c28e68f3eea4beb
Successfully built a7a858cbf4dd
Successfully tagged registry.ocp4.example.com/demo/quarkus-func-test:latest
🙌 Function image built: registry.ocp4.example.com/demo/quarkus-func-test:latest
```
The "-p" parameter is needed if command line above doesn't run in func directory. The container image named "registry.ocp4.example.com/demo/quarkus-func-test:latest" will be built, its name is based on "-r" parameter and func name.  

If the development environment is in disconnected environment, then s2i builder images must be in local registry, and func dependencies must be in local maven repository. Local registry must support anonymous access and local maven repository must be indicated in pom.xml file:  
```
kn func build -p quarkus-func-test -r registry.ocp4.example.com/demo -b s2i --builder-image registry.ocp4.example.com/demo/openjdk-17:v1 -v
```
kn using local registry pull secret feature is on the roadmap...  

After build stage, there is container image named "registry.ocp4.example.com/demo/quarkus-func-test:latest", copy it to disconnected environment and then push into local registry:  
```
podman images
REPOSITORY                                                            TAG                           IMAGE ID      CREATED         SIZE
registry.ocp4.example.com/demo/quarkus-func-test                      latest                        a7a858cbf4dd  38 minutes ago  558 MB
...

podman push registry.ocp4.example.com/demo/quarkus-func-test:latest
```

## Deploy the knative func to openshift cluster
When the func container image is in local registry, the func can be deployed to openshift cluster.  
At first, a local registry pull secret must be created and linked to a service account named "default" in target namespace, please reference [Pulling from private registries with delegated authentication](https://docs.openshift.com/container-platform/4.12/openshift_images/managing_images/using-image-pull-secrets.html#images-pulling-from-private-registries_using-image-pull-secrets):  
```
oc get secret quay-registry -o yaml -n test 
apiVersion: v1
data:
  .dockerconfigjson: ...
kind: Secret
...
type: kubernetes.io/dockerconfigjson


oc get serviceaccount default -o yaml -n test
apiVersion: v1
imagePullSecrets:
- name: default-dockercfg-hwdxn
- name: quay-registry
kind: ServiceAccount
...
secrets:
- name: default-dockercfg-hwdxn
```

Second, the func directory with func.yaml file should be copied into disconnected environment. "kn" and "oc" tools are ready.  
```
cat quarkus-func-test/func.yaml 
specVersion: 0.35.0
name: quarkus-func-test
runtime: quarkus
registry: registry.ocp4.example.com/demo
image: registry.ocp4.example.com/demo/quarkus-func-test:latest
created: 2024-02-26T17:01:38.548983305+08:00
build:
  builder: s2i
  buildEnvs:
  - name: BP_JVM_VERSION
    value: "17"
  - name: BP_NATIVE_IMAGE
    value: "true"
  - name: BP_MAVEN_BUILT_ARTIFACT
    value: func.yaml target/native-sources/*
  - name: BP_MAVEN_BUILD_ARGUMENTS
    value: package -DskipTests=true -Dmaven.javadoc.skip=true -Dquarkus.package.type=native-sources
  - name: BP_NATIVE_IMAGE_BUILD_ARGUMENTS_FILE
    value: native-image.args
  - name: BP_NATIVE_IMAGE_BUILT_ARTIFACT
    value: '*-runner.jar'
  pvcSize: 256Mi

oc project
Using project "test" on server "https://api.ocp4.example.com:6443".
```

And then deploy the func using kn command:  
```
kn func deploy --build=false --push=false -n test -p quarkus-func-test -r registry.ocp4.example.com/demo -v
  Deploying function to the cluster
Waiting for Knative Service to become ready
Function deployed in namespace "test" and exposed at URL:
https://quarkus-func-test-test.apps.ocp4.example.com
✅ Function deployed in namespace "test" and exposed at URL: 
   https://quarkus-func-test-test.apps.ocp4.example.com

kn func list
NAME               NAMESPACE  RUNTIME  URL                                                   READY
quarkus-func-test  test       quarkus  https://quarkus-func-test-test.apps.ocp4.example.com  True

oc get pod -n test
No resources found in test namespace.
```
After deploying, pods of the func are not start, they start and run when messages arrive automatically.  


## Test the knative func
Follow README.md file to sent a message to the func...  
```
curl -k https://quarkus-func-test-test.apps.ocp4.example.com/?message=World
{"message":"Hello World!"}

oc get pod -n test
NAME                                                  READY   STATUS    RESTARTS   AGE
quarkus-func-test-00001-deployment-669c4757f5-4wqgs   2/2     Running   0          50s
```
Later, pods of the func will stop automatically to save the resources.  
