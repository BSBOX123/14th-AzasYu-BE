# AzasYu Backend

멋쟁이사자처럼 영남대학교 14기 해커톤 프로젝트 **AzasYu**의 백엔드 저장소입니다.

## 💡 프로젝트 소개

팀 내에서 명확하게 표현되지 않은 의견과 가짜 합의를 줄이고, 구성원들의 실제 생각을 안전하게 공유할 수 있도록 돕는 AI 기반 협업 서비스입니다.

### 핵심 기능

- 회원가입 및 로그인
- 프로젝트 생성 및 참여
- AI 사전 인터뷰
- 익명 의견 공유
- 회의 내용 기록
- AI 기반 모호한 표현 탐지
- 프로젝트 및 회의 결과 관리

<br>

## ✂️ Team

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

<br>

## 🛠 기술 스택
추후 업데이트 예정.

### Backend

- Language: Java 21
- Framework: Spring Boot 4.1
- Database: MySQL 8.4, Spring Data JPA
- Authentication: Spring Security, JWT

### AI

- AI Model: 환경 변수로 설정
- AI API: Google Gemini API (`gemini-3.5-flash-lite`)

### Infrastructure

- Cloud: AWS
- Database: MySQL
- Container: Docker

## 💻 로컬 실행

```bash
./gradlew bootRun
```

- 기본 로컬 실행은 별도 설치가 필요 없는 파일형 H2 DB를 사용합니다.
- 로컬 DB 파일은 `data/`에 생성되며 Git에 포함되지 않습니다.
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health check: `http://localhost:8080/actuator/health`
- H2 Console: `http://localhost:8080/h2-console`
- 테스트: `./gradlew test`

배포 환경에서는 `SPRING_PROFILES_ACTIVE=prod`와 DB 환경변수를 설정해 MySQL을 사용합니다.

API 및 도메인 경계는 [`docs/MVP_DOMAIN.md`](docs/MVP_DOMAIN.md)를 참고합니다.

<br>

## ▶️ 실행 방법

AzasYu는 배포된 웹 서비스로 이용할 수 있습니다.

1. 아래 서비스 주소에 접속합니다.
2. 회원가입 또는 로그인을 진행합니다.
3. 새로운 프로젝트를 생성하거나 참여 코드를 입력해 기존 프로젝트에 참여합니다.
4. 프로젝트에 입장한 후 AI 사전 인터뷰, 익명 의견 공유 및 회의 기능을 이용합니다.

### 서비스 주소

- Web: 배포 후 추가 예정
- Backend: 배포 후 추가 예정

> 현재 서비스는 개발 중이며, 배포가 완료되면 접속 주소를 업데이트할 예정입니다.

<br>

## 🌿 브랜치 전략

원본 저장소의 `main` 브랜치에는 직접 push하지 않습니다. 모든 기능 개발과 문서 수정은 개인 Fork 저장소의 작업 브랜치에서 진행하며, 작업이 완료되면 Pull Request를 생성하여 팀원의 확인을 받은 후 반영합니다.

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

<br>

## 📝 커밋 전략

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

<br>

## 🚀 배포 주소

배포 완료 후 주소를 추가합니다.

- Backend:
- Frontend:

<br>

## 📄 라이선스

추후 업데이트 예정입니다.
