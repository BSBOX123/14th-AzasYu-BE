package com.azasyu.domain.project;

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
    name = "project_members",
    uniqueConstraints = @UniqueConstraint(name = "uk_project_members_project_user", columnNames = {"project_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String role;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    public ProjectMember(Project project, User user, ProjectMemberRole role) {
        this.project = project;
        this.user = user;
        this.role = role.name();
    }
}
