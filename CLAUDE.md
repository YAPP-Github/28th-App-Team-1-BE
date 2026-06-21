# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 아키텍처

Spring Boot + Java 기반 헥사고날 아키텍처(Ports and Adapters) 프로젝트입니다.
**코드 작성 전 반드시 `ARCHITECTURE.md`를 읽으세요.** 패키지 구조, 네이밍 규칙, 레이어별 책임, 금지 패턴이 모두 정리되어 있습니다.

핵심 규칙:
- 의존 방향은 `Adapter → Application → Domain` 을 절대 준수합니다.
- Domain 객체는 순수 비즈니스 모델 — JPA·Spring 어노테이션 금지
- JPA 엔티티는 `adapter/out/persistence/entity/` 에만 위치합니다.
- Command 객체는 `application/command/` 에 위치합니다.
- 설정 클래스는 `common/config/` 에 위치합니다.
- Service는 유스케이스 단위로 분리합니다. (`MemberRegisterService` O, `MemberService` X)

## 빌드 & 실행

```bash
# 전체 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 단일 테스트 클래스 실행
./gradlew test --tests "com.example.member.application.service.MemberRegisterServiceTest"

# 애플리케이션 실행
./gradlew bootRun
```

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
