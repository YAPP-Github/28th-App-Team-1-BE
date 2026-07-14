# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

AI 기반 모의 면접 코칭 서비스의 백엔드(Spring Boot + Java 21). 사용자가 포트폴리오(PDF)와 채용 공고(JD)를 업로드하면, 이를 임베딩/분석해 맞춤 면접 질문을 생성하고 면접 세션을 진행·채점하는 것이 핵심 도메인입니다.

## 아키텍처

헥사고날 아키텍처(Ports and Adapters)를 적용합니다.
**코드 작성 전 반드시 `ARCHITECTURE.md`를 읽으세요.** 패키지 구조, 네이밍 규칙, 레이어별 책임, 금지 패턴이 모두 정리되어 있습니다.

핵심 규칙:
- 의존 방향은 `Adapter → Application → Domain` 을 절대 준수합니다.
- Domain 객체는 순수 비즈니스 모델 — JPA·Spring 어노테이션 금지
- JPA 엔티티는 `adapter/out/persistence/entity/` 에만 위치합니다.
- Command 객체는 `application/command/` 에 위치합니다.
- 설정 클래스는 `common/config/` 에 위치합니다.
- Service는 유스케이스 단위로 분리합니다. (`MemberRegisterService` O, `MemberService` X)

### 도메인 모듈 (`src/main/java/com/yapp/d14/`)

| 모듈 | 책임 |
|---|---|
| `auth` | 카카오/애플 소셜 로그인, JWT 발급 |
| `user` | 사용자 도메인 |
| `portfolio` | 포트폴리오 PDF 업로드·텍스트 추출·청크 임베딩(pgvector) |
| `jd` | 채용 공고(JD) 크롤링·추출·키워드 분석 |
| `job` | 직무/직군 참조 데이터 |
| `interview` | 면접 세션 진행, 질문 생성(preload), 답변 채점(axes/rubric), TTS |
| `ticket` | 이용권·예약(hold) 관리 |
| `common` | 공통 설정, 응답 포맷, 예외 기반 클래스, JWT 필터, `@CurrentUser` 리졸버 |

`interview` 모듈의 평가 축(axis)·루브릭 정의는 코드가 아니라 `src/main/resources/interview-rubric/`에서 관리합니다. 리소스는 사용 시점에 따라 두 갈래로 나뉩니다.

- **면접 진행 중**(캐물지점 추출·axis 태깅·천장 판별): `axes.yaml`, `principles.yaml`, `ceiling-fewshot.md`
- **채점·리포트 생성 시**: `scoring-bars.yaml`(6대 축별 4점 BARS 앵커), `red-flags.yaml`(레드플래그 카탈로그)

면접 중 프롬프트에는 axes.yaml만 캐싱·재사용되므로, 채점 전용 기준(BARS·레드플래그)은 별도 파일로 분리해 관심사를 섞지 않습니다. 채점/면접 로직을 다룰 때는 해당 시점의 리소스를 함께 확인하세요.

## 기술 스택

- Java 21, Spring Boot 3.4.5, Spring Security, Spring Data JPA
- PostgreSQL + `pgvector` (포트폴리오 임베딩 저장), Redis
- JWT (`jjwt`), 소셜 로그인(Kakao, Apple)
- Spring AI: OpenAI(임베딩/JD 추출/키워드 추출/TTS), Anthropic(면접 preload 캐물지점 추출)
- PDFBox + Tika(PDF 페이지 검증/텍스트 추출), Jsoup(JD 크롤링)
- AWS S3 (파일 저장)

## 빌드 & 실행

```bash
# 전체 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 단일 테스트 클래스 실행
./gradlew test --tests "com.example.member.application.service.MemberRegisterServiceTest"

# 애플리케이션 실행 (로컬 인프라 필요: postgres, redis)
./gradlew bootRun

# 로컬 인프라(Postgres+pgvector, Redis) 기동
docker compose up -d postgres redis

# 테스트용 JWT 액세스 토큰 발급 (-PuserId, -Pprovider, -PexpiryMs)
./gradlew issueTestToken -PuserId=1 -Pprovider=KAKAO
```

- 로컬 개발 시 `.env` 파일의 값이 `spring-dotenv`를 통해 자동 로드됩니다.
- Swagger UI(`springdoc`)는 기본 `application.yml`에서 비활성화되어 있으며 `dev` 프로파일 등에서 별도로 켭니다.
- `application-dev.yml`은 `ddl-auto: update`를 사용합니다 — 엔티티 변경 시 스키마 마이그레이션 여부를 함께 확인하세요.

## Git 협업 규칙

GitHub Flow를 사용합니다. `main` 브랜치만 영구 유지하며, 이슈 생성 → 브랜치 → PR → 머지 순서로 진행합니다.

**브랜치 명명**
```
feat/{이슈번호}-{기능명}
fix/{이슈번호}-{버그명}
chore/{이슈번호}-{내용}
```

**커밋 메시지**
```
<type>: <변경 요약 (한글 또는 영문, 50자 이내)>
```

| 타입 | 용도 |
|---|---|
| `feat` | 새로운 기능 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변경 없는 코드 개선 |
| `docs` | 문서 작성·수정 |
| `test` | 테스트 추가·수정 |
| `chore` | 빌드·설정 등 기타 |

- 브랜치 prefix와 커밋 타입은 독립적입니다. (`feat/` 브랜치에서 `fix:` 커밋 가능)
- 커밋 메시지에 이슈번호를 포함하지 않습니다.
- PR 본문에 `closes #번호`를 명시하면 머지 시 이슈가 자동으로 닫힙니다.
