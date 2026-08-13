package com.azasyu.domain.meeting;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingAgendaRepository extends JpaRepository<MeetingAgenda, Long> {

    List<MeetingAgenda> findAllByMeetingIdOrderByAgendaOrderAsc(Long meetingId);
}
