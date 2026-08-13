# MVP 도메인 및 API 경계

## 핵심 도메인

- `User`: 회원 및 인증 정보
- `Project`, `ProjectMember`: 프로젝트와 참여 코드 기반 구성원 관계
- `Meeting`, `MeetingAgenda`, `MeetingParticipant`: 회의 정보, 복수 안건, 참여자
- `InterviewQuestionSet`, `InterviewQuestion`: 회의별 공통 질문 묶음과 개별 질문
- `InterviewSubmission`, `InterviewAnswer`: 참여자의 답변 제출 단위와 질문별 답변
- `IdeaCard`: 참여자 답변에서 생성한 개인 의견 요약
- `IdeaSummary`: 새로고침 시점의 전체 카드 분석 결과
- `MeetingRecord`: 직접 입력하거나 문서에서 추출한 회의 원문
- `MeetingAnalysis`: 회의 목적, 주요 논의, 결정 및 추가 확인 내용
- `AmbiguityFinding`: 모호한 문장과 탐지 이유

## MVP API 그룹

| 그룹 | 책임 |
|---|---|
| `/api/v1/auth` | 회원가입, 로그인 |
| `/api/v1/projects` | 생성, 참여 코드 참가, 목록 및 상세 조회 |
| `/api/v1/projects/{id}/meetings` | 프로젝트별 회의 생성 및 목록 조회 |
| `/api/v1/meetings/{id}` | 회의 상세 조회 (안건·참여자 포함) |
| `/api/v1/meetings/{id}/interview` | 공통 질문 조회 및 재생성, 답변 제출, 카드 생성 |
| `/api/v1/meetings/{id}/idea-cards` | 익명 카드 조회 |
| `/api/v1/meetings/{id}/idea-summary` | 최신 전체 요약 조회 및 명시적 새로고침 |
| `/api/v1/meetings/{id}/record` | 텍스트 입력 및 TXT/DOCX/PDF 업로드 |
| `/api/v1/meetings/{id}/result` | 회의 분석과 모호성 탐지 결과 조회 및 재생성 |

회의의 안건과 참여자는 별도 API 없이 회의 생성 시 함께 등록한다.

AI 생성이 필요한 자원(공통 질문, 아이디어 카드, 회의 분석)은 생성 실패 시 상태가
`FAILED`로 저장되며, 각 그룹의 재생성 API로 다시 시도한다.

미정 정책은 엔티티 제약으로 고정하지 않는다. 특히 카드 개수, 답변 수정, 관리자 세부 권한, 음성/STT와 모호성 후속 조치는 후속 결정 전까지 확장하지 않는다.
