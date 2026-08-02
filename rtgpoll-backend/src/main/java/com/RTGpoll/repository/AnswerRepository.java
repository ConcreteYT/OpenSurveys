package com.RTGpoll.repository;

import com.RTGpoll.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Persistence for submitted answers.
 * {@code findAllByQuestionFormIdOrderByIdAsc} powers GET /forms/{id}/responses
 * aggregation in FormController.
 */
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findAllByQuestionFormIdOrderByIdAsc(Long formId);
}
