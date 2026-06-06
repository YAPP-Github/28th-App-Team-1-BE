# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Architecture

This is a Spring Boot multi-module project using Hexagonal Architecture (Ports and Adapters).
**Read `ARCHITECTURE.md` before writing any code.** All structural rules, naming conventions, package layouts, and anti-patterns are documented there.

Key points:
- Dependency direction is strictly `Adapter → Application → Domain`
- Domain modules (`domain-member`, `domain-post`) depend only on `common`
- Domain objects are pure business models — no JPA, no Spring annotations
- JPA entities live in `adapter/out/persistence/entity/`
- Command objects live in `application/command/`
- Config classes live in `common/config/`
- Services are split per use case (e.g., `MemberRegisterService`, not `MemberService`)

## Build & Run

```bash
# 전체 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 특정 모듈 테스트
./gradlew :domain-member:test

# 단일 테스트 클래스 실행
./gradlew :domain-member:test --tests "com.example.member.application.service.MemberRegisterServiceTest"

# 애플리케이션 실행
./gradlew :app:bootRun
```
