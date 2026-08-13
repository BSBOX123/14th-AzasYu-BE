package com.azasyu.domain.meeting;

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
@Table(name = "meeting_agendas")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(nullable = false)
    private Integer agendaOrder;

    @Column(nullable = false, length = 500)
    private String content;

    public MeetingAgenda(Meeting meeting, int agendaOrder, String content) {
        this.meeting = meeting;
        this.agendaOrder = agendaOrder;
        this.content = content;
    }
}
