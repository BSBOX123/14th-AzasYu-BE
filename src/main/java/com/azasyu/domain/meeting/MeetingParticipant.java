package com.azasyu.domain.meeting;

import com.azasyu.domain.user.User;
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
    name = "meeting_participants",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_meeting_participants_meeting_user",
        columnNames = {"meeting_id", "user_id"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public MeetingParticipant(Meeting meeting, User user) {
        this.meeting = meeting;
        this.user = user;
    }
}
