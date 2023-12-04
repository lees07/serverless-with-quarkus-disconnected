package com.redhat.developers.microsweeper.service;

import com.redhat.developers.microsweeper.model.Score;
import com.redhat.developers.microsweeper.model.ScoreRepository;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

import org.springframework.web.context.annotation.ApplicationScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ApplicationScope
@Component
public class ScoreboardService  {
	@Autowired
    ScoreRepository scoreRepository;

    public List<Score> getScoreboard() {
        return scoreRepository.findAll();
    }

    @Transactional
    @Timed(description = "How log to add a score", value = "scoreboard-timer")
    public void addScore(Score score) {
        Timer timer = Metrics.globalRegistry.timer("example.prime.number.test");
        timer.record(() -> {
            scoreRepository.save(score);
        });
    }

    @Transactional
    public long clearScores() {
        scoreRepository.deleteAll();
        return 0;
    }

}
