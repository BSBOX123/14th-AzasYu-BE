package com.azasyu.domain.interview.entity;

import com.azasyu.domain.meeting.entity.Meeting;
import com.azasyu.domain.user.User;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Entity
@Table(
    name = "interview_submissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_interview_submissions_meeting_user",
        columnNames = {"meeting_id", "user_id"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String cardGenerationStatus;

    @Column(length = 500)
    private String failureMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    public InterviewSubmission(Meeting meeting, User user) {
        this.meeting = meeting;
        this.user = user;
        this.cardGenerationStatus = QuestionGenerationStatus.PENDING.name();
    }

    public void generated() {
        cardGenerationStatus = QuestionGenerationStatus.GENERATED.name();
        failureMessage = null;
    }

    public void failed(String message) {
        cardGenerationStatus = QuestionGenerationStatus.FAILED.name();
        failureMessage = message;
    }

    public void notConfigured() {
        cardGenerationStatus = QuestionGenerationStatus.NOT_CONFIGURED.name();
        failureMessage = "Gemini API 키가 설정되지 않았습니다.";
    }

    public void pending() {
        cardGenerationStatus = QuestionGenerationStatus.PENDING.name();
        failureMessage = null;
    }
}
