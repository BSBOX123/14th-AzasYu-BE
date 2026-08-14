package com.azasyu.domain.interview.ai;

import com.azasyu.domain.interview.entity.InterviewAnswer;

/**
 * AI 클라이언트에 전달하는 인터뷰 답변 한 건.
 *
 * <p>{@code InterviewAnswer}의 질문 연관은 지연 로딩이라 트랜잭션 밖에서 읽을 수 없다.
 */
public record InterviewAnswerContext(String question, String answer) {

    public static InterviewAnswerContext from(InterviewAnswer answer) {
        return new InterviewAnswerContext(answer.getQuestion().getContent(), answer.getContent());
    }
}
