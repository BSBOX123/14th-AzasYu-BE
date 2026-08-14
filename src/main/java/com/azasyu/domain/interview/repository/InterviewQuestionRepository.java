package com.azasyu.domain.interview.repository;

import com.azasyu.domain.interview.entity.InterviewQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    List<InterviewQuestion> findAllByQuestionSetIdOrderByQuestionOrderAsc(Long questionSetId);

    /**
     * 기존 질문을 즉시 삭제함.
     *
     * <p>파생 삭제는 DELETE를 flush 시점까지 미루고, Hibernate는 flush 시 INSERT를
     * DELETE보다 먼저 실행한다. 재생성 시 같은 {@code (question_set_id, question_order)}로
     * INSERT가 먼저 나가 {@code uk_interview_questions_order} 제약을 위반하므로
     * 벌크 삭제로 바꿔 DELETE가 먼저 실행되게 함.
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from InterviewQuestion q where q.questionSet.id = :questionSetId")
    void deleteAllByQuestionSetId(@Param("questionSetId") Long questionSetId);
}
