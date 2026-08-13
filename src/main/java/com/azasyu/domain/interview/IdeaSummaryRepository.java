package com.azasyu.domain.interview;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdeaSummaryRepository extends JpaRepository<IdeaSummary, Long> {

    Optional<IdeaSummary> findFirstByMeetingIdOrderByVersionDesc(Long meetingId);
}
