package com.redhat.developers.microsweeper.model;

import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;

@Entity
@Table(name="score")
public class Score {
    @Id @GeneratedValue
    private Long id;
    @Column
    private long scoreId;
    @Column
    private String name;
    @Column
    private String level;
    @Column
    private int time;
    @Column
    private boolean success;

    public Score() {}

    public Score(Long id, long scoreId, String name, String level, int time, boolean success) {
        this.id = id;
        this.scoreId = scoreId;
        this.name = name;
        this.level = level;
        this.time = time;
        this.success = success;
    }

    public Long getId() {
        return id;
    }

    public long getScoreId() {
        return scoreId;
    }

    public String getName() {
        return name;
    }

    public String getLevel() {
        return level;
    }

    public int getTime() {
        return time;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setId(Long id) {
        this.id = id;
    } 

    public void setScoreId(long scoreId) {
        this.scoreId = scoreId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return name + "/" + level + "/" + time + "/" + success + "/" + scoreId;
    }
}
