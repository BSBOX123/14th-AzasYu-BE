package com.azasyu.domain.interview.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "interview_questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_set_id", nullable = false)
    private InterviewQuestionSet questionSet;

    @Column(nullable = false)
    private Integer questionOrder;

    @Column(nullable = false, length = 1000)
    private String content;

    public InterviewQuestion(InterviewQuestionSet questionSet, int questionOrder, String content) {
        this.questionSet = questionSet;
        this.questionOrder = questionOrder;
        this.content = content;
    }
}
