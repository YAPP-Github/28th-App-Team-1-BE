# ARCHITECTURE.md — 아키텍처 & 개발 규칙

> Spring Boot + Java 기반 프로젝트입니다.
> 헥사고날 아키텍처(Ports and Adapters)를 적용합니다.

---

## 1. 아키텍처 핵심 규칙

### 의존 방향 (절대 규칙)

```
Adapter → Application → Domain
```

- 역방향 의존은 절대 금지합니다.
- Application Core(Application + Domain)는 외부 기술(Spring Web, AWS, Spring Security 등)을 직접 알면 안 됩니다.
- Application은 반드시 Port(인터페이스)에만 의존하고, Adapter 구현체를 직접 참조하지 않습니다.

---

## 2. 패키지 구조

```
src/main/java/com/example/{name}/
    ├── domain/                        # 헥사곤 내부 — 순수 비즈니스 모델 (JPA 어노테이션 없음)
    ├── application/
    │   ├── command/                   # Command 객체 (UseCase 진입 시 사용)
    │   ├── port/
    │   │   ├── in/                    # Port(in): 외부 → 헥사곤 진입점 인터페이스
    │   │   │   └── result/            # UseCase 반환값 객체 (Result, Summary 등)
    │   │   └── out/                   # Port(out): 헥사곤 → 외부 요청 인터페이스
    │   └── service/                   # Port(in) 구현체 (유스케이스 로직)
    ├── exception/                     # 도메인 전용 예외
    └── adapter/
        ├── in/
        │   └── web/                   # Primary Adapter: HTTP Controller
        │       ├── request/           # HTTP 요청 DTO
        │       └── response/          # HTTP 응답 DTO
        └── out/
            ├── persistence/           # Secondary Adapter: DB
            │   └── entity/            # JPA Entity 클래스
            ├── integration/           # Secondary Adapter: 외부 시스템 (S3, Lambda 등)
            │   └── aws/
            └── security/              # Security Adapter
```

---

## 3. Common 패키지 구조

```
src/main/java/com/example/common/
    ├── config/                        # 공통 설정 (AWS, 외부 기술 Config 클래스)
    ├── exception/
    │   ├── BusinessException.java     # 모든 도메인 예외의 기반 클래스
    │   └── ErrorCode.java             # 에러 코드 인터페이스
    ├── response/
    │   ├── ApiResponse.java           # 정상 응답 포맷
    │   ├── ErrorResponse.java         # 예외 응답 포맷
    │   └── PageResponse.java          # 페이지네이션 응답 포맷
    └── util/
        ├── DateTimeUtils.java
        └── StringUtils.java
```

---

## 4. 파일명 규칙

### Domain (`domain/`)

| 유형 | 규칙 | 예시 |
|---|---|---|
| 도메인 객체 (순수 비즈니스 모델) | `{명사}.java` | `Member.java` |
| 값 객체 | `{명사}.java` | `Email.java`, `Name.java` |
| 상태 enum | `{명사}Status.java` | `MemberStatus.java` |

### Application (`application/`)

| 유형 | 규칙 | 예시 |
|---|---|---|
| 커맨드 객체 (`command/`) | `{명사}{동작}Command.java` | `MemberRegisterCommand.java` |
| Port(in) 인터페이스 | `{명사}{동작}UseCase.java` | `MemberRegisterUseCase.java` |
| Port(in) 반환값 객체 (`port/in/result/`) | `{명사}{동작}Result.java` | `MemberRegisterResult.java` |
| Port(out) 저장소 인터페이스 | `{명사}Repository.java` | `MemberRepository.java` |
| Port(out) 외부 기능 인터페이스 | `{기능명}.java` | `EmailSender.java`, `PasswordEncoder.java` |
| 서비스 구현체 (커맨드) | `{명사}{동작}Service.java` | `MemberRegisterService.java` |
| 서비스 구현체 (쿼리) | `{명사}QueryService.java` | `MemberQueryService.java` |

### Adapter (`adapter/`)

| 유형 | 규칙 | 예시 |
|---|---|---|
| 웹 컨트롤러 | `{명사}Controller.java` | `MemberController.java` |
| HTTP 요청 DTO | `{명사}{동작}HttpRequest.java` | `MemberRegisterHttpRequest.java` |
| HTTP 응답 DTO | `{명사}{동작}HttpResponse.java` | `MemberDetailHttpResponse.java` |
| JPA 엔티티 (`persistence/entity/`) | `{명사}JpaEntity.java` | `MemberJpaEntity.java` |
| 외부 시스템 Adapter | `{기술}{역할}Adapter.java` | `SmtpEmailSenderAdapter.java` |
| Security Adapter | `{기술}{역할}Adapter.java` | `BcryptPasswordEncoderAdapter.java` |

### Common (`common/`)

| 유형 | 규칙 | 예시 |
|---|---|---|
| 설정 클래스 (`config/`) | `{기술}Config.java` | `AwsConfig.java` |
| 공통 예외 기반 | `BusinessException.java` | — |
| 에러 코드 인터페이스 | `ErrorCode.java` | — |
| 도메인 예외 기반 | `{명사}Exception.java` | `MemberException.java` |
| 도메인 에러 코드 enum | `{명사}ErrorCode.java` | `MemberErrorCode.java` |

---

## 5. 레이어별 책임 규칙

### Domain
- 핵심 비즈니스 규칙과 상태만 담습니다.
- JPA 어노테이션(`@Entity`, `@Column` 등)은 금지합니다. 순수 비즈니스 모델만 위치합니다.
- JPA 엔티티는 `adapter/out/persistence/entity/`에 별도로 위치합니다.
- `@RestController`, `@Service` 등 Spring 웹/서비스 어노테이션은 금지합니다.
- HTTP, AWS, Spring Security, JPA 등 외부 기술 의존은 모두 금지합니다.
- 비즈니스 로직은 Domain 클래스 안에 작성합니다. (정적 팩토리 메서드, 상태 변경 메서드 등)

### Application Service
- Port(in) 인터페이스를 구현합니다.
- Port(out) 인터페이스에만 의존하고, Adapter 구현체를 직접 참조하지 않습니다.
- `S3Client`, `BCryptPasswordEncoder` 등 외부 기술을 직접 사용하지 않습니다.
- 비즈니스 규칙을 직접 갖기보다 Domain 객체들을 조율해 업무 흐름을 완성합니다.
- **기능(유스케이스) 단위로 Service를 분리합니다.** `MemberService` 하나에 모든 기능을 몰아넣지 않습니다.

```
✅ MemberRegisterService  — 등록만
✅ MemberQueryService     — 조회만
✅ MemberModifyService    — 수정만
✅ MemberWithdrawService  — 탈퇴만

❌ MemberService          — 모든 기능 집합
```

### Controller (Primary Adapter)
- Port(in) UseCase 인터페이스 타입으로 주입받습니다. Service 구현체를 직접 주입받지 않습니다.
- HTTP 요청 DTO → Command 변환은 DTO의 `toCommand()` 메서드에서 수행합니다.
- Domain 객체를 그대로 반환하지 않고, 반드시 Response DTO로 변환합니다.

### Secondary Adapter
- Port(out) 인터페이스를 구현합니다.
- 외부 기술(SMTP, S3, BCrypt 등)은 이 레이어에서만 사용합니다.

---

## 6. DTO 위치 규칙

| DTO 종류 | 위치 |
|---|---|
| HTTP 요청 DTO | `adapter/in/web/request/` |
| HTTP 응답 DTO | `adapter/in/web/response/` |
| 커맨드 객체 | `application/command/` |
| Port(in) 반환값 객체 | `application/port/in/result/` |
| JPA 엔티티 | `adapter/out/persistence/entity/` |
| QueryDSL Projection DTO | `adapter/out/persistence/` |
| 외부 시스템 연동 DTO | `adapter/out/integration/` |
| 공통 응답 포맷 | `common/response/` |
| 설정 클래스 | `common/config/` |

---

## 7. 유효성 검증 규칙

- **HTTP DTO**: `@NotBlank`, `@Email` 등 Bean Validation 어노테이션으로 형식 검증
- **Command 객체**: 생성자에서 비즈니스 제약 조건 강제 (null 체크, 길이 제한 등)
- Application Service는 Command 객체를 신뢰하고 비즈니스 로직만 수행합니다.

---

## 8. 접근 제어 규칙

| 대상 | 접근 수준 |
|---|---|
| `adapter/` 내 클래스 전체 | `package-private` |
| `application/port/in`, `application/port/out` | `public` |
| `domain/` 클래스 | `public` |
| `application/service/` 구현체 | `package-private` (권장) |

---

## 9. 자주 하는 실수 — 절대 하지 말 것

```
❌ Controller에서 Domain 객체를 그대로 반환
   → Response DTO로 변환 후 반환

❌ Application Service에서 JpaRepository 직접 @Autowired
   → Port(out) 인터페이스(MemberRepository)를 주입

❌ Domain에 @Entity, @Column 등 JPA 어노테이션 추가
   → Domain은 순수 비즈니스 모델, JPA 엔티티는 adapter/out/persistence/entity/에 위치

❌ Domain에 @RestController, @Service 등 Spring 어노테이션 추가
   → Domain에는 어떠한 외부 기술 어노테이션도 금지

❌ Application Service에서 S3Client, BCryptPasswordEncoder 직접 사용
   → Port(out) 인터페이스 정의 후 Adapter에서 구현

❌ Domain에 @JsonProperty 등 HTTP 관련 어노테이션 추가
   → HTTP DTO는 adapter/in/web/에서 별도 관리

❌ domain 패키지에서 common 외 다른 모듈 import
   → domain은 common만 의존 (JPA도 금지)

❌ MemberService 하나에 등록/조회/수정/탈퇴 모두 구현
   → 기능 단위로 Service 분리

❌ Controller에서 Service 구현체를 직접 주입
   → 반드시 UseCase 인터페이스 타입으로 주입

❌ PasswordEncoder를 Domain에 위치 (Spring Security 의존 발생)
   → application/port/out/에 인터페이스 정의, adapter/out/security/에서 구현

❌ AWS, DB 등 기술 설정 클래스를 adapter/ 내부에 위치
   → 설정 클래스는 common/config/에 위치

❌ Command 객체를 domain/ 패키지에 위치
   → application/command/에 위치
```

---

## 10. 요청 처리 흐름 요약

```
[HTTP 요청]
    ↓
Controller (adapter/in/web)
  HTTP 요청 DTO 수신 → toCommand() → Command 객체 생성
    ↓
Port(in) UseCase 인터페이스 (application/port/in)
    ↓
Application Service (application/service)
  비즈니스 로직 수행, Port(out) 인터페이스 호출
    ↓
Port(out) 인터페이스 (application/port/out)
    ↓
Secondary Adapter (adapter/out)
  DB, 메일, AWS 등 외부 인프라 처리
    ↓
[외부 인프라]
```
