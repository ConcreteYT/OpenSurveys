package com.opensurveys.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.opensurveys.model.Question;

// Not directly used by FormController today - Questions are persisted via Form's
// cascade=ALL relationship (see Form.questions) - but provided for direct lookups/future
// use (e.g. an endpoint to fetch/edit a single question).
public interface QuestionRepository extends JpaRepository<Question, Long> {
}
