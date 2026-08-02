package com.RTGpoll.repository;

import com.RTGpoll.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

// Not directly used by FormController today - Questions are persisted via Form's
// cascade=ALL relationship (see Form.questions) - but provided for direct lookups/future
// use (e.g. an endpoint to fetch/edit a single question).
public interface QuestionRepository extends JpaRepository<Question, Long> {
}
