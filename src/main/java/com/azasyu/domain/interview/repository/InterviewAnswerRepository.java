package com.azasyu.domain.interview.repository;

import com.azasyu.domain.interview.entity.InterviewAnswer;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {

    @EntityGraph(attributePaths = "question")
    List<InterviewAnswer> findAllBySubmissionIdOrderByQuestionQuestionOrderAsc(Long submissionId);
}
