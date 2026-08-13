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
@Table(name = "ambiguity_findings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AmbiguityFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private MeetingAnalysis analysis;

    @Column(nullable = false)
    private Integer findingOrder;

    @Column(nullable = false, length = 2000)
    private String expression;

    @Column(nullable = false, length = 4000)
    private String reason;

    public AmbiguityFinding(MeetingAnalysis analysis, int findingOrder, String expression, String reason) {
        this.analysis = analysis;
        this.findingOrder = findingOrder;
        this.expression = expression;
        this.reason = reason;
    }
}
