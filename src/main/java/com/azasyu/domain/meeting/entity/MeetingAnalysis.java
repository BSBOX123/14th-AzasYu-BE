package com.azasyu.domain.meeting.entity;

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

/**
 * 회의 원문에서 추출한 요약과 모호성 탐지 결과.
 *
 * <p>AI 생성 상태를 {@link MeetingAnalysisStatus}로 관리함. 상태 전이 메서드로만 값을 바꾸며,
 * 실패해도 레코드를 지우지 않고 상태만 FAILED로 내려 재시도할 수 있게 함.
 */
@Getter
@Entity
@Table(name = "meeting_analyses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingAnalysis {

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

    @Column(length = 4000)
    private String meetingPurpose;

    @Column(columnDefinition = "TEXT")
    private String keyDiscussions;

    @Column(columnDefinition = "TEXT")
    private String decisions;

    @Column(columnDefinition = "TEXT")
    private String followUpChecks;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public MeetingAnalysis(Meeting meeting) {
        this.meeting = meeting;
        this.status = MeetingAnalysisStatus.PENDING.name();
    }

    public void pending() {
        status = MeetingAnalysisStatus.PENDING.name();
        failureMessage = null;
    }

    public void generated(String purpose, String discussions, String decisions, String checks) {
        status = MeetingAnalysisStatus.GENERATED.name();
        failureMessage = null;
        meetingPurpose = purpose;
        keyDiscussions = discussions;
        this.decisions = decisions;
        followUpChecks = checks;
    }

    public void failed(String message) {
        status = MeetingAnalysisStatus.FAILED.name();
        failureMessage = message;
    }

    public void notConfigured() {
        status = MeetingAnalysisStatus.NOT_CONFIGURED.name();
        failureMessage = "Gemini API 키가 설정되지 않았습니다.";
    }
}
