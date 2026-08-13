package com.azasyu.domain.interview;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {

    @EntityGraph(attributePaths = "question")
    List<InterviewAnswer> findAllBySubmissionIdOrderByQuestionQuestionOrderAsc(Long submissionId);
}
