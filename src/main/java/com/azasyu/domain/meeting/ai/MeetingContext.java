package com.azasyu.domain.meeting.ai;

import com.azasyu.domain.meeting.entity.Meeting;

/**
 * AI 클라이언트에 전달하는 회의 정보.
 *
 * <p>AI 호출은 트랜잭션 밖에서 이루어지므로 JPA 엔티티를 그대로 넘기면
 * 지연 로딩 필드 접근 시 {@code LazyInitializationException}이 발생한다.
 * 트랜잭션 안에서 필요한 값만 추출해 이 타입으로 전달한다.
 */
public record MeetingContext(String title, String purpose) {

    public static MeetingContext from(Meeting meeting) {
        return new MeetingContext(meeting.getTitle(), meeting.getPurpose());
    }
}
