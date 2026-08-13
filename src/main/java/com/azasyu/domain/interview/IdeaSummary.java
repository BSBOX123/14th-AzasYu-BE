package com.azasyu.domain.interview;

import com.azasyu.domain.meeting.Meeting;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Entity
@Table(name = "idea_summaries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdeaSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "refreshed_by", nullable = false)
    private User refreshedBy;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false)
    private Integer sourceCardCount;

    @Column(nullable = false, length = 4000)
    private String commonOpinions;

    @Column(nullable = false, length = 4000)
    private String differingOpinions;

    @Column(nullable = false, length = 4000)
    private String keyConcerns;

    @Column(nullable = false, length = 4000)
    private String discussionPoints;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public IdeaSummary(
        Meeting meeting, User refreshedBy, int version, int sourceCardCount,
        String commonOpinions, String differingOpinions, String keyConcerns, String discussionPoints
    ) {
        this.meeting = meeting;
        this.refreshedBy = refreshedBy;
        this.version = version;
        this.sourceCardCount = sourceCardCount;
        this.commonOpinions = commonOpinions;
        this.differingOpinions = differingOpinions;
        this.keyConcerns = keyConcerns;
        this.discussionPoints = discussionPoints;
    }
}
