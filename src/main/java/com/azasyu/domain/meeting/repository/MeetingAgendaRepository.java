package com.azasyu.domain.meeting.repository;

import com.azasyu.domain.meeting.entity.MeetingAgenda;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingAgendaRepository extends JpaRepository<MeetingAgenda, Long> {

    List<MeetingAgenda> findAllByMeetingIdOrderByAgendaOrderAsc(Long meetingId);
}
