package com.azasyu.domain.meeting.repository;

import com.azasyu.domain.meeting.entity.MeetingParticipant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

    @EntityGraph(attributePaths = "user")
    List<MeetingParticipant> findAllByMeetingIdOrderByIdAsc(Long meetingId);

    /**
     * 회의별 참여자 수를 한 번에 센다.
     *
     * <p>목록에서 회의마다 참여자를 조회해 크기를 세면 N+1이 된다. 엔티티를 불러오지 않고
     * 집계만 가져온다. 결과는 {@code [회의 id, 참여자 수]} 배열이다.
     */
    @Query("select p.meeting.id, count(p) from MeetingParticipant p "
        + "where p.meeting.id in :meetingIds group by p.meeting.id")
    List<Object[]> countByMeetingIds(@Param("meetingIds") Collection<Long> meetingIds);

    /** 주어진 회의 중 해당 사용자가 참여자로 등록된 회의 id만 반환한다. */
    @Query("select p.meeting.id from MeetingParticipant p "
        + "where p.meeting.id in :meetingIds and p.user.id = :userId")
    Set<Long> findParticipatingMeetingIds(
        @Param("meetingIds") Collection<Long> meetingIds,
        @Param("userId") Long userId
    );
}
