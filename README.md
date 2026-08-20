# AzasYu Backend

멋쟁이사자처럼 영남대학교 14기 해커톤 프로젝트 **AzasYu**의 백엔드 저장소입니다.

## 프로젝트 소개

팀 내에서 명확하게 표현되지 않은 의견과 가짜 합의를 줄이고, 구성원들의 실제 생각을
안전하게 공유할 수 있도록 돕는 AI 기반 협업 서비스입니다.

회의 전에는 참여자에게 공통 질문을 던져 의견을 미리 모으고, 모인 의견은 익명 카드로만
공개합니다. 회의 후에는 회의록에서 모호한 표현을 찾아 다시 확인해야 할 지점을 짚어 줍니다.

### 서비스 흐름

백엔드는 아래 순서를 하나의 회의 단위로 처리합니다.

```
1. 가입과 로그인
2. 프로젝트 생성 또는 참여 코드로 참가
3. 회의 생성. 안건과 참여자를 등록하고 참여 코드를 발급합니다
       3-1. AI 공통 질문 자동 생성
       3-2. 참여자별 답변 제출. 제출한 참여자의 아이디어 카드를 생성합니다
       3-3. 익명 아이디어 보드에서 카드 열람
       3-4. 전체 의견 요약 생성
4. 회의 원문 등록. 직접 입력하거나 파일을 올립니다
       4-1. AI 회의 분석과 모호한 표현 탐지
```

### 주요 기능

- 회원가입과 로그인. JWT 기반의 무상태 인증을 사용합니다.
- 프로젝트 생성과 참여. 8자리 참여 코드로 팀원을 초대합니다.
- 회의 생성과 합류. 회의도 프로젝트와 별개인 참여 코드를 가집니다.
- AI 사전 인터뷰. 회의 제목과 목적, 안건을 바탕으로 공통 질문을 생성합니다.
- 익명 아이디어 보드. 응답에 작성자를 식별할 수 있는 값을 담지 않습니다.
- 전체 의견 요약. 공통 의견, 다른 의견, 우려, 논의할 점으로 나누어 정리합니다.
- 회의 원문 기록. 직접 입력과 TXT, DOCX, PDF 업로드를 지원합니다.
- 회의 분석과 모호성 탐지. 모호한 문장과 그 이유를 함께 제시합니다.

## Team

<table>
  <tr>
    <th align="center">FE</th>
    <th align="center">FE</th>
    <th align="center">FE</th>
    <th align="center">BE · AI</th>
    <th align="center">BE · AI</th>
  </tr>
  <tr>
    <td align="center">
      <a href="https://github.com/Wooong-E">
        <img src="https://github.com/Wooong-E.png" width="120px" alt="JoWoong 프로필"/>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/kang-bs">
        <img src="https://github.com/kang-bs.png" width="120px" alt="Boseong Kang 프로필"/>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/njm0927">
        <img src="https://github.com/njm0927.png" width="120px" alt="Jaemin 프로필"/>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/thstmddn321">
        <img src="https://github.com/thstmddn321.png" width="120px" alt="손승우 프로필"/>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/BSBOX123">
        <img src="https://github.com/BSBOX123.png" width="120px" alt="김승윤 프로필"/>
      </a>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="https://github.com/Wooong-E"><strong>JoWoong</strong></a>
    </td>
    <td align="center">
      <a href="https://github.com/kang-bs"><strong>Boseong Kang</strong></a>
    </td>
    <td align="center">
      <a href="https://github.com/njm0927"><strong>Jaemin</strong></a>
    </td>
    <td align="center">
      <a href="https://github.com/thstmddn321"><strong>손승우</strong></a>
    </td>
    <td align="center">
      <a href="https://github.com/BSBOX123"><strong>김승윤</strong></a>
    </td>
  </tr>
</table>

## 백엔드 구조

메인 소스 100개, 테스트 12개, 컨트롤러 8개, 엔드포인트 25개, 엔티티 15개로 구성되어 있습니다.

### 패키지 구조

기능은 `domain`, 공통 기반은 `global`로 나눕니다.

```
com.azasyu
    domain
        auth         회원가입, 로그인
        user         User 엔티티. 다른 도메인이 참조합니다
        project      프로젝트, 구성원
        meeting      회의, 원문, 분석
        interview    공통 질문, 답변, 아이디어 카드, 요약
    global
        config       보안, Swagger, 프로퍼티 설정
        security     JWT 발급과 검증
        ai           Gemini 호출 공통 계층
        api          공통 응답 형식
        error        예외와 전역 처리기
        support      참여 코드 생성기
```

`project`, `meeting`, `interview`는 규모가 커서 내부를 역할별로 다시 나눕니다.

```
meeting
    controller    HTTP 매핑과 API 문서
    service       비즈니스 로직과 트랜잭션 경계
    entity        JPA 엔티티와 상태 열거형
    repository    Spring Data JPA
    dto           요청과 응답. 모두 record입니다
    ai            해당 도메인이 사용하는 AI 클라이언트
```

`auth`와 `user`는 파일 수가 적어 나누지 않았습니다.

### 요청 처리 흐름

```
HTTP 요청
    1. JwtAuthenticationFilter    Authorization 헤더를 읽어 사용자 ID를 보관합니다
    2. SecurityConfig             공개 경로 외에는 인증을 요구합니다
    3. Controller                 요청 DTO를 검증하고 사용자 ID를 받습니다
    4. Service                    권한을 확인하고 로직을 수행합니다
    5. Repository                 데이터베이스에 접근합니다
    6. 공통 응답 형식으로 감싸 반환합니다
```

예외가 발생하면 `GlobalExceptionHandler`가 같은 형식으로 변환합니다.

## 공통 규약

### 응답 형식

모든 응답은 동일한 형태로 감싸 반환합니다. 성공과 실패 중 한쪽만 채워집니다.

```json
{ "success": true, "data": { } }
```

```json
{ "success": false, "error": { "code": "MEETING_NOT_FOUND", "message": "회의를 찾을 수 없습니다." } }
```

같은 상태 코드가 여러 상황에서 쓰이므로, 클라이언트는 상태 코드보다 `error.code`로
분기하는 편이 안전합니다.

### 인증

로그인에 성공하면 액세스 토큰을 발급합니다. 이후 요청은 `Authorization` 헤더에
`Bearer` 형식으로 토큰을 담아 보냅니다.

무상태 설계이므로 서버에 보관하는 세션이 없습니다. 로그아웃과 토큰 재발급 API는 두지
않았으며, 클라이언트가 토큰을 폐기하면 로그아웃으로 처리합니다.

로그인 실패는 계정이 없는 경우와 비밀번호가 다른 경우를 같은 오류로 응답합니다.
가입 여부가 드러나지 않도록 하기 위함입니다.

### 권한 기준

프로젝트 구성원과 회의 참여자는 다른 개념이며, API마다 요구하는 기준이 다릅니다.

| 대상 | 필요 권한 |
|---|---|
| 프로젝트 상세와 목록, 회의 상세와 목록 | 프로젝트 구성원 |
| 회의 참여 코드 합류, 공통 질문 재생성 | 프로젝트 구성원 |
| 공통 질문 조회, 인터뷰 제출, 아이디어 보드 | 회의 참여자 |
| 회의 원문 등록과 조회, 분석 결과 | 회의 참여자 |
| 아이디어 카드 수정과 삭제 | 해당 카드의 작성자 |

권한이 없으면 자원의 존재를 숨기기 위해 403이 아니라 404를 반환합니다.
예외는 요청자에게 다음 행동을 알려야 하는 두 가지입니다.

| 상황 | 응답 |
|---|---|
| 프로젝트에 참여하지 않은 사용자가 회의 참여 코드로 합류 | 403 `PROJECT_MEMBER_REQUIRED` |
| 다른 참여자의 아이디어 카드를 수정하거나 삭제 | 403 `IDEA_CARD_FORBIDDEN` |

## AI 연동

프롬프트를 다루는 도메인 클라이언트와 HTTP 호출만 담당하는 공통 클라이언트로 나눕니다.

```
1. 도메인 AI 클라이언트    프롬프트 구성, 응답 스키마 정의, 파싱
2. GeminiApiClient         HTTP 호출
3. Gemini generateContent API
```

| 도메인 클라이언트 | 생성 대상 |
|---|---|
| `OpenAiInterviewQuestionClient` | 사전 인터뷰 공통 질문 |
| `OpenAiIdeaCardClient` | 개인 아이디어 카드 |
| `OpenAiIdeaSummaryClient` | 전체 의견 요약 |
| `OpenAiMeetingAnalysisClient` | 회의 분석과 모호성 탐지 |

각 클라이언트는 인터페이스와 구현으로 나뉘어 있어 테스트에서 대체할 수 있습니다.
응답 구조는 Gemini의 JSON 스키마 기능으로 고정하여 정의하지 않은 필드가 섞이지 않도록 합니다.

### 트랜잭션 경계

AI 호출은 응답이 늦어질 수 있으므로 트랜잭션 밖에서 수행합니다. 데이터베이스 커넥션을
오래 점유하지 않기 위함입니다.

```
1. 짧은 트랜잭션    생성 중 상태를 저장하고 커밋합니다
2. 트랜잭션 밖      AI를 호출합니다
3. 짧은 트랜잭션    결과를 저장하고 커밋합니다
```

경계는 `TransactionTemplate`으로 명시합니다. 트랜잭션 밖에서는 지연 로딩을 쓸 수 없으므로
AI 클라이언트에는 엔티티 대신 값 객체를 전달합니다.

### 생성 상태

AI 생성물은 실패하더라도 요청 전체를 실패시키지 않고 상태로 남깁니다. 클라이언트는 상태를
보고 재생성 버튼을 노출할 수 있습니다.

| 상태 | 의미 |
|---|---|
| `PENDING` | 생성 중입니다 |
| `GENERATED` | 생성이 완료되었습니다. 이때만 본문이 채워집니다 |
| `FAILED` | 생성에 실패했습니다. 재생성 API로 다시 시도합니다 |
| `NOT_CONFIGURED` | 서버에 Gemini API 키가 설정되지 않았습니다 |

호출 타임아웃은 연결 5초, 읽기 60초이며 환경변수로 조정할 수 있습니다.

## 데이터베이스

스키마는 Flyway가 관리하고 JPA는 검증만 수행합니다. 따라서 엔티티를 변경하면 마이그레이션도
함께 추가해야 합니다. 이미 적용된 마이그레이션 파일은 수정하지 않습니다.

| 버전 | 내용 |
|---|---|
| V1 | `users` |
| V2 | `projects`, `project_members` |
| V3 | `meetings`, `meeting_agendas`, `meeting_participants` |
| V4 | `interview_question_sets`, `interview_questions` |
| V5 | `interview_submissions`, `interview_answers`, `idea_cards` |
| V6 | `idea_summaries` |
| V7 | `meeting_records` |
| V8 | `meeting_analyses`, `ambiguity_findings` |
| V9 | 회의 참여 코드 |
| V10 | 아이디어 카드 수정 시각과 노출 여부 |
| V11 | 프로젝트 카드 색상 |

아이디어 카드 삭제는 행을 제거하지 않고 노출 여부만 내립니다. 인터뷰 답변 원문을 보존하기
위함입니다.

## API 개요

상세 명세는 Swagger UI에서 확인할 수 있습니다.

| 그룹 | 책임 |
|---|---|
| `/api/v1/auth` | 회원가입, 로그인 |
| `/api/v1/projects` | 생성, 참여 코드 참가, 목록과 상세 조회 |
| `/api/v1/projects/{id}/meetings` | 프로젝트별 회의 생성과 목록 조회 |
| `/api/v1/meetings/join` | 회의 참여 코드로 합류 |
| `/api/v1/meetings/{id}` | 회의 상세 조회 |
| `/api/v1/meetings/{id}/interview` | 공통 질문 조회와 재생성, 답변 제출, 카드 생성 |
| `/api/v1/meetings/{id}/idea-cards` | 익명 카드 조회, 내 카드 수정과 삭제 |
| `/api/v1/meetings/{id}/idea-summary` | 최신 요약 조회와 새로고침 |
| `/api/v1/meetings/{id}/record` | 텍스트 입력과 파일 업로드 |
| `/api/v1/meetings/{id}/result` | 회의 분석과 모호성 탐지 결과 조회, 재생성 |

도메인 경계와 정책은 [`docs/MVP_DOMAIN.md`](docs/MVP_DOMAIN.md)에 정리되어 있습니다.

## 기술 스택

### Backend

- Language: Java 21
- Framework: Spring Boot 4.1
- Database: MySQL 8.4 (운영), H2 (로컬과 테스트)
- Persistence: Spring Data JPA, Flyway
- Authentication: Spring Security, JWT
- Documentation: springdoc-openapi

### AI

- AI API: Google Gemini API
- 기본 모델: `gemini-3.5-flash-lite`. 환경변수 `GEMINI_MODEL`로 변경할 수 있습니다

### Infrastructure

- Cloud: AWS EC2 (Ubuntu 24.04)
- Container: Docker, Docker Compose
- CI: GitHub Actions. 테스트와 MySQL 기동 검증을 수행합니다

## 로컬 실행

```bash
./gradlew bootRun
```

별도 설치 없이 파일형 H2 데이터베이스를 사용합니다. 데이터베이스 파일은 `data/`에 생성되며
저장소에는 포함되지 않습니다.

| 항목 | 주소 |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |
| H2 Console | http://localhost:8080/h2-console |

테스트는 아래 명령으로 실행합니다.

```bash
./gradlew test
```

기본 프로필은 `local`입니다. 배포 환경에서는 `SPRING_PROFILES_ACTIVE=prod`와 데이터베이스
환경변수를 설정하여 MySQL을 사용합니다.

## 실행 방법

AzasYu는 배포된 웹 서비스로 이용할 수 있습니다.

1. 아래 서비스 주소에 접속합니다.
2. 회원가입 또는 로그인을 진행합니다.
3. 새로운 프로젝트를 생성하거나 참여 코드를 입력해 기존 프로젝트에 참여합니다.
4. 프로젝트에 입장한 후 AI 사전 인터뷰, 익명 의견 공유 및 회의 기능을 이용합니다.

### 서비스 주소

| 구분 | 주소 |
|---|---|
| Web | http://3.39.194.205 |
| Backend | http://15.165.87.189:8080 |
| API 문서 | http://15.165.87.189:8080/swagger-ui.html |

HTTPS는 아직 적용하지 않았습니다.

## 배포

`compose.prod.yml`로 EC2에 배포합니다. 환경변수는 `.env.example`을 `.env`로 복사해 채웁니다.

```bash
docker compose -f compose.prod.yml up -d --build
```

스키마는 애플리케이션이 기동할 때 Flyway가 자동으로 적용합니다.

## 브랜치 전략

원본 저장소의 `main` 브랜치에는 직접 push하지 않습니다. 모든 기능 개발과 문서 수정은
개인 Fork 저장소의 작업 브랜치에서 진행하며, 작업이 완료되면 Pull Request를 생성하여
팀원의 확인을 받은 후 반영합니다.

### 브랜치 이름

```text
타입/작업내용
```

| 타입 | 용도 | 예시 |
|---|---|---|
| `feat` | 새로운 기능 개발 | `feat/project-create` |
| `fix` | 오류 수정 | `fix/login-error` |
| `docs` | 문서 작성 및 수정 | `docs/readme` |
| `refactor` | 기능 변경 없는 코드 개선 | `refactor/meeting-service` |
| `test` | 테스트 코드 작성 및 수정 | `test/project-service` |
| `chore` | 설정 및 기타 작업 | `chore/deploy-config` |

### 작업 브랜치 생성

작업을 시작하기 전에 원본 저장소의 최신 내용을 반영합니다.

```bash
git switch main
git fetch upstream
git merge upstream/main
```

새로운 작업 브랜치를 생성합니다.

```bash
git switch -c docs/readme
```

## 커밋 전략

커밋 메시지는 다음 형식을 사용합니다.

```text
타입: 변경 내용
```

| 타입 | 설명 |
|---|---|
| `feat` | 새로운 기능 추가 |
| `fix` | 오류 수정 |
| `docs` | README 등 문서 변경 |
| `refactor` | 기능 변경 없는 코드 개선 |
| `test` | 테스트 코드 추가 및 수정 |
| `chore` | 빌드, 배포 및 설정 변경 |

### 커밋 메시지 예시

```text
docs: README 작성
feat: 프로젝트 생성 기능 구현
feat: 참여 코드 검증 기능 구현
fix: 로그인 토큰 검증 오류 수정
refactor: 회의 서비스 로직 분리
test: 프로젝트 생성 테스트 추가
chore: 배포 설정 추가
```

### 커밋 작성 원칙

- 하나의 커밋에는 하나의 목적만 담습니다.
- 변경 내용을 이해할 수 있도록 구체적으로 작성합니다.
- 의미 없는 메시지는 사용하지 않습니다.

```text
# 권장
feat: 프로젝트 참여 코드 생성 기능 구현

# 지양
수정
작업함
최종
진짜 최종
```

## 라이선스

추후 업데이트 예정입니다.
