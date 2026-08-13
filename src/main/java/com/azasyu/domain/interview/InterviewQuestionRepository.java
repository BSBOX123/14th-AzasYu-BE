package com.azasyu.domain.interview;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    List<InterviewQuestion> findAllByQuestionSetIdOrderByQuestionOrderAsc(Long questionSetId);

    void deleteAllByQuestionSetId(Long questionSetId);
}
