package com.azasyu.domain.interview.entity;

import com.azasyu.domain.meeting.entity.Meeting;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(name = "interview_question_sets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewQuestionSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false, unique = true)
    private Meeting meeting;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 500)
    private String failureMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public InterviewQuestionSet(Meeting meeting) {
        this.meeting = meeting;
        this.status = QuestionGenerationStatus.PENDING.name();
    }

    public void generated() {
        this.status = QuestionGenerationStatus.GENERATED.name();
        this.failureMessage = null;
    }

    public void failed(String message) {
        this.status = QuestionGenerationStatus.FAILED.name();
        this.failureMessage = message;
    }

    public void notConfigured() {
        this.status = QuestionGenerationStatus.NOT_CONFIGURED.name();
        this.failureMessage = "Gemini API 키가 설정되지 않았습니다.";
    }

    public void pending() {
        this.status = QuestionGenerationStatus.PENDING.name();
        this.failureMessage = null;
    }
}
