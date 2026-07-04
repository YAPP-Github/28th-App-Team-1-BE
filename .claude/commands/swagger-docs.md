# 스웨거 문서화 스킬

지정한 Controller의 `XxxControllerDocs` 인터페이스를 작성하거나 갱신한다.

인자: `$ARGUMENTS` — 대상 Controller 파일명 또는 경로 (예: `MemberController` 또는 전체 경로)

## 실행 순서

1. `$ARGUMENTS`로 대상 `XxxController.java`를 찾는다. 파일명만 주어졌다면 Glob으로 실제 경로를 찾는다. 못 찾으면 사용자에게 경로를 확인한다.
2. Controller를 읽고 각 핸들러 메서드에서 다음을 파악한다:
   - HTTP 메서드/경로 (`@GetMapping`, `@PostMapping` 등)
   - 요청/응답 타입 (`RequestBody`, `ApiResponse<T>` 등)
   - 인증 필요 여부 — `@CurrentUser` 파라미터가 있으면 인증 필요, 없으면 불필요
3. 같은 도메인 패키지의 `exception/XxxErrorCode.java`를 찾아 이 Controller가 실제로 던질 수 있는 에러 코드, HTTP 상태, 메시지를 수집한다.
   - 어떤 코드가 이 API에서 발생하는지 애매하면 서비스 계층(유스케이스 구현체)까지 확인한다.
   - `ErrorCode` enum에 없는 코드를 지어내지 않는다. 필요한 코드가 없으면 사용자에게 먼저 확인한다.
4. `src/main/java/com/yapp/d14/auth/adapter/in/web/AuthControllerDocs.java`를 레퍼런스 패턴으로 삼아 다음 스타일을 그대로 따라 `XxxControllerDocs.java`를 작성한다:
   - 인터페이스 레벨 `@Tag(name, description)`
   - 메서드별 `@Operation(summary, description)` — `description`은 마크다운으로 작성하고 반드시 `**인증**: 필요/불필요` 문구와 호출 조건·제약(TTL, rotation 등 실제 동작)을 포함한다.
   - `@ApiResponses`에 성공 케이스(200/201/204)와 실제 발생 가능한 에러 케이스를 모두 나열한다. 에러 응답은 `@ExampleObject`로 `{"success": false, "code": "...", "message": "..."}` JSON을 하드코딩한다 (`ErrorCode` enum 값 그대로 사용).
   - 인증 불필요 API에는 `@SecurityRequirements`를 붙인다.
   - `@CurrentUser`처럼 내부 주입되는 파라미터는 `@Parameter(hidden = true)`로 숨긴다.
5. Controller가 아직 Docs 인터페이스를 `implements`하지 않았다면 `implements XxxControllerDocs`를 추가하고, 각 메서드에 `@Override`를 붙인다.
6. 작성 결과를 요약해 보여준다 (어떤 에러 코드를 어디서 가져왔는지 포함). 파일 변경은 git으로 되돌릴 수 있으므로 바로 저장하되, 임의로 지어낸 부분이 있다면 반드시 짚어서 알린다.

## 규칙

- Docs 인터페이스 파일은 Controller와 같은 디렉토리(`adapter/in/web/`)에 둔다.
- Domain 객체, Command, 실제 비즈니스 로직은 건드리지 않는다 — Docs 인터페이스 작성과 Controller의 `implements`/`@Override` 추가만 한다.
- 이미 `XxxControllerDocs`가 존재하면 새로 만들지 않고 기존 내용을 갱신한다 (누락된 메서드 추가, 실제와 어긋난 설명 수정).
- 에러 코드/메시지는 항상 실제 `ErrorCode` enum 값을 근거로 작성한다. 추측 금지.

## 실행

위 순서대로 바로 실행한다.
