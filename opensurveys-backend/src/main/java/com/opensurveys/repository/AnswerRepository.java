package com.opensurveys.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.opensurveys.model.Answer;

import java.util.List;

/**
 * Persistence for submitted answers.
 */
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    /** Full answers with their questions, used by the export. */
    @EntityGraph(attributePaths = "question")
    List<Answer> findAllByQuestionFormIdOrderByIdAsc(Long formId);

    /** Only (questionId, answer) pairs, used by GET /forms/{id}/responses aggregation. */
    @Query("select q.id as questionId, a.answer as answer from Answer a join a.question q "
            + "where q.form.id = :formId order by a.id asc")
    List<QuestionAnswer> findQuestionAnswersByFormId(@Param("formId") Long formId);

    /**
     * Answers of the given question type that contain {@code fragment} as a substring.
     * Callers must still check the exact file name; LIKE only narrows the candidates.
     */
    @Query("select a.answer from Answer a join a.question q "
            + "where q.form.id = :formId and q.questionType = :questionType "
            + "and a.answer like concat('%', :fragment, '%')")
    List<String> findAnswersContaining(@Param("formId") Long formId,
                                       @Param("questionType") Integer questionType,
                                       @Param("fragment") String fragment);

    interface QuestionAnswer {
        Long getQuestionId();

        String getAnswer();
    }
}
