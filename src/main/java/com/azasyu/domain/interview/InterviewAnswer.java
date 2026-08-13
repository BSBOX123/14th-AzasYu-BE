package com.azasyu.domain.interview;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "interview_answers",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_interview_answers_submission_question",
        columnNames = {"submission_id", "question_id"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private InterviewSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private InterviewQuestion question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public InterviewAnswer(InterviewSubmission submission, InterviewQuestion question, String content) {
        this.submission = submission;
        this.question = question;
        this.content = content;
    }
}
