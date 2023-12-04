package com.redhat.developers.microsweeper;

import com.redhat.developers.microsweeper.model.Score;
import com.redhat.developers.microsweeper.service.ScoreboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/scoreboard")
public class ScoreboardResource {
    @Autowired
    ScoreboardService scoreboardService;

    final Logger logger = LoggerFactory.getLogger(getClass());

    @GetMapping
    public List<Score> getScoreboard() {
        return scoreboardService.getScoreboard();
    }

    @PostMapping
    public void addScore(@RequestBody Score score) throws Exception {
        scoreboardService.addScore(score);
    }

    @DeleteMapping
    public long clearAll() {
        return scoreboardService.clearScores();
    }

}
