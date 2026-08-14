package com.azasyu.domain.interview.entity;

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

@Getter
@Entity
@Table(name = "idea_cards")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdeaCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private InterviewSubmission submission;

    @Column(nullable = false, length = 1000)
    private String coreOpinion;

    @Column(nullable = false, length = 2000)
    private String rationale;

    @Column(nullable = false, length = 2000)
    private String concern;

    @Column(nullable = false, length = 2000)
    private String alternative;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public IdeaCard(
        InterviewSubmission submission,
        String coreOpinion,
        String rationale,
        String concern,
        String alternative
    ) {
        this.submission = submission;
        this.coreOpinion = coreOpinion;
        this.rationale = rationale;
        this.concern = concern;
        this.alternative = alternative;
    }
}
