package com.redhat.developers.microsweeper.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository  
public interface ScoreRepository extends JpaRepository<Score, Long> {}