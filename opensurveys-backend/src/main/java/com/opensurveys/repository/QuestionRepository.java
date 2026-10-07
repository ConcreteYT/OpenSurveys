package com.opensurveys.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.opensurveys.model.Question;

// Questions are normally persisted via Form's cascade=ALL relationship (see Form.questions);
// this is used for direct lookups such as FormSubmissionController#stageQuestionFiles.
public interface QuestionRepository extends JpaRepository<Question, Long> {
}
