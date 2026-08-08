# S3 저장소 정책

> 이 문서는 S3에 저장되는 파일들의 경로 규칙과 생명주기(만료·삭제) 정책을 정의한다.
> 코드 작업 시 이 문서를 기준으로 삼는다. 정책이 바뀌면 이 문서를 먼저 갱신한 뒤 코드를 맞춘다.

## 1. 버킷 구조

버킷은 비공개(private)이며, 모든 파일 접근은 Presigned URL을 통해서만 이루어진다.

```
{bucket}/
  ├─ users/{userId}/
  │     │
  │     ├─ portfolios/
  │     │     └─ {portfolioId}.pdf         ← 원본 포트폴리오 파일
  │     │
  │     └─ sessions/
  │           └─ {sessionId}/
  │                 │
  │                 ├─ answers/            ← 사용자 답변 음성 (제출 시 비동기 저장, 리포트 영상 합성용, 네이티브 iOS/Android 기본 포맷 m4a. 합성 성공 시 삭제 — §3.6)
  │                 │     └─ {turnLevel}.m4a
  │                 │
  │                 ├─ questions/          ← AI 면접관 TTS 음성
  │                 │     └─ {turnLevel}.mp3
  │                 │
  │                 ├─ recording/          ← 면접 영상 (프론트 녹화본, 네이티브 iOS/Android 기본 포맷 mp4)
  │                 │     └─ raw.mp4
  │                 │
  │                 └─ composite/          ← 최종 합성 영상
  │                       └─ final.mp4
  │
  └─ system/
        └─ interview/
              └─ wrapup-messages/          ← 세션 종료 마무리 멘트 TTS(공용, 특정 유저 소유 아님)
                    └─ {endType}.mp3         (MANUAL_END / HARD_CAP / NORMAL_END)
```

`system/` 하위는 특정 유저·세션에 속하지 않는 공용 자산이다. 마무리 멘트 문구는 종료 사유별로 고정돼 있으므로, 최초 요청 시 TTS로 합성해 업로드하고 이후에는 캐시 조회(cache-miss 시에만 재생성)한다. 3장의 세션 영상 만료·삭제 정책과는 무관하며, 문구가 바뀌지 않는 한 삭제되지 않는다.

## 2. 접근 정책

- 버킷은 private. 모든 객체는 Presigned URL로만 접근 가능하다.
- Presigned URL 자체의 서명 유효시간(TTL)은 아래 3장의 `expires_at`/`base_at` 기준 만료(비즈니스 만료 시점)과 **별개의 값**이다. 링크 자체는 짧게(예: 5~10분), 콘텐츠 접근 가능 기간은 별도 정책(3장)을 따른다.

## 3. 인터뷰 세션 영상 만료·삭제 정책

### 3.1 원칙

- S3에는 영상을 저장만 하고, S3 자체의 만료(Lifecycle) 설정은 사용하지 않는다.
- 실제 접근 가능 여부는 `interview_video` 테이블의 `expires_at`(소유자 화면 기준) 또는 `base_at`(지인 접근 기준) 값으로 제어한다.
- 물리적 삭제는 별도 배치가 지연 처리한다 (접근 제어와 스토리지 정리를 분리).
- 소유자(면접자 본인)가 자기 리포트에서 영상을 보는 흐름과, 지인이 공유 링크로 영상을 보거나 피드백을 제출하는 흐름은
  **서로 다른 만료 판정 기준**을 쓴다(3.2, 3.3). 두 기준 모두 물리 삭제 시점(3.4)에는 영향을 주지 않는다 —
  물리 삭제는 어느 한쪽이라도 유효하면 지연된다는 뜻이 아니라, 아직 별도 배치가 구현되지 않은 상태다.

### 3.2 소유자(면접자 본인) 화면 — 단계형 만료 연장 규칙 (기획에 따라 변경 가능성 있음)

리포트에서 본인이 자기 영상을 보거나(`InterviewReportQueryService`), 공유 상태를 조회할 때(`FeedbackShareQueryService`)
적용되는 기준이다. `base_at`(1차 리포트 생성 성공 시각) 기준으로 아래 트리거가 연장되며, 항상 더 긴 쪽만 반영한다
(`InterviewVideo.extend()`, `VideoRetentionTrigger`).

| 트리거 | 계산식 |
|---|---|
| 기본 (1차 리포트 생성 시 1회) | `base_at` + 24h |
| 피드백 요청 전송(공유 링크 생성) | `base_at` + 48h |
| 지인 최초 조회 | `base_at` + 7일 |
| 지인 최초 제출 | `base_at` + 30일 |

### 3.3 지인(공유 링크) 접근 — 30일 하드캡

지인이 공유 링크로 영상을 재생하거나(`GuestFeedbackQueryService.enter()`) 피드백을 제출할 때
(`GuestFeedbackSubmitService`)는 위 3.2의 단계형 `expires_at`을 보지 않는다. 공유 링크 생성 후 지인이 늦게
접속하면(예: 48h 경과 후 최초 접속) 3.2 기준으로는 이미 만료 판정이 나서 접근이 영구 차단되는 문제가 있었기 때문에,
지인 쪽은 **`base_at` + 30일(영상 최대보유기간) 하드캡**으로만 판정한다(`InterviewVideo.isExpiredForGuest()`).
소유자 화면의 단계형 `expires_at`은 지인의 조회·제출 이벤트가 발생하면 3.2의 트리거를 통해 그대로 연장된다
(지인 쪽 판정 자체가 바뀌었을 뿐, 연장 트리거·소유자 화면 동작은 변경 없음).

```
지인 재생/제출 요청
  └─ base_at + 30일 > NOW() 확인
       ├─ 이내 → Presigned URL 생성/제출 허용
       └─ 초과 → 영상 만료 응답
```

### 3.4 영상 URL 제공 흐름 (소유자 화면)

```
expires_at > NOW() 확인 (3.2 단계형 기준)
  ├─ 유효 → Presigned URL 생성 후 반환
  └─ 만료 → 영상 만료 응답
```

### 3.5 실제 삭제 배치 (매일 자정, 미구현 — 설계만 존재)

```
대상: expires_at < NOW() AND deleted = FALSE
동작: S3 파일 삭제 → deleted = TRUE 업데이트
```

- S3 key는 `userId` + `sessionId`로부터 결정적으로 계산 가능하므로, `interview_video` 테이블에 별도 S3 key 컬럼 없이 배치에서 재계산해 삭제한다. (세션당 영상이 1개라는 전제 하에 유효)
- 삭제 대상 범위(⚠️ 확인 필요): `composite/final.mp4`만 삭제할지, `sessions/{sessionId}/` 하위 전체(`questions/`, `recording/`, `composite/`)를 함께 삭제할지 정해야 한다. 원본 녹화본도 개인정보이므로 함께 정리하는 편이 자연스럽다. (`answers/`는 §3.6에서 합성 성공 시 이미 삭제되므로 이 배치의 대상이 아니다.)
- S3에 폴더 단위 삭제 API는 없으므로, 하위 전체를 지운다면 `ListObjectsV2` + `DeleteObjects`(배치 삭제)로 구현해야 한다. 단일 객체만 지운다면 `DeleteObject`로 충분하다.
- 배치는 최대 24h 지연될 수 있지만, 3.4의 API 레벨 체크(`expires_at > NOW()`)가 이미 접근을 차단하므로 사용자 노출 관점에서는 문제가 없다. 순수 스토리지 비용 정리 목적의 지연이다.
- ⚠️ 이 배치가 실제로 구현되면, `expires_at < NOW()`가 지인 하드캡(`base_at`+30일)보다 먼저 도달할 수 있다는 점을 확인해야
  한다 — 소유자 화면 기준으로는 만료돼도 지인은 여전히 30일 이내라면 접근 가능해야 하므로(3.3), 삭제 대상 조건에
  `base_at + 30일 < NOW()`도 함께 만족하는지 확인하는 조건 추가가 필요하다.

### 3.6 합성 성공 시 답변 음성 즉시 삭제 (구현됨)

면접자 답변 음성(`answers/{turnLevel}.m4a`)은 **사용자의 실제 목소리(개인정보)**이며, 유일한 소비처가 리포트 영상 합성 한 곳뿐이다. 합성으로 만든 `composite/final.mp4`에 목소리가 이미 얹히면 원본은 중복 데이터가 되므로, `InterviewVideoCompositeService`가 합성에 성공한 직후(`markComposited > 0`) 해당 세션의 `answers/` 프리픽스 하위를 즉시 삭제한다(`InterviewVoiceStorage.deleteAnswerAudio`).

- 합성 실패(`catch`)나 표시할 영상 row가 없는 경우(`marked == 0`)에는 삭제하지 않는다 — 재합성 여지를 남긴다.
- 삭제는 부가 정리라 실패해도 합성 성공을 되돌리지 않는다. 예외를 삼키고 로깅만 하며, 남은 파일은 §5(리포트 없이 끝난 세션) 또는 (미래) §3.5 배치가 회수한다.
- 질문 TTS(`questions/{turnLevel}.mp3`)는 대상이 아니다 — AI 합성음이라 개인정보 민감도가 낮고, 리포트 세그먼트 생성(`QuestionUtteranceSegmentPersister`)이 합성과 별개 흐름으로 재소비하기 때문이다.

## 4. 포트폴리오 파일 생명주기

- 업로드: `POST /api/v1/portfolios` 등록 시 비동기로 S3 업로드 (`PortfolioProcessService`, `S3PortfolioFileUploaderAdapter`).
- 처리 타임아웃: `PortfolioProcessService`는 실패·취소 경로마다 S3 원본과 pgvector 청크를 직접 정리하지만, 워커 스레드가 죽거나 앱이 재시작되면 그 정리 코드 자체가 실행되지 않는다. 이때는 상태 조회·목록 조회가 타임아웃을 감지하는 시점(`PortfolioProcessingTimeoutHandler`)이 유일한 회수 지점이다 — `FAILED_SYSTEM` 전환과 함께 pgvector 청크를 같은 트랜잭션에서 지우고, S3 원본은 커밋 이후 삭제한다.
  - 실패한 포트폴리오는 소프트 삭제하지 않는다. 실패 사유가 목록·상태 조회에 계속 보여야 하고, 신규 등록 차단 기준(`existsActiveByUserId`)이 `READY`·미삭제만 보므로 실패 건이 남아 있어도 재업로드를 막지 않는다.
- 삭제: 포트폴리오 삭제는 **소프트 삭제**다. `portfolios` row는 삭제 월 1회 제한과 재업로드 월 1회 제한 판정을 위한 이력으로 보존되고(`deleted=true`, `deleted_at` 기록), S3 원본 파일과 pgvector 임베딩은 이력 보존과 무관하게 즉시 물리 삭제된다. `PortfolioDeleteService.delete()`는 `@Transactional`로 DB 갱신(소프트 삭제 `save`)을 감싸고, `AfterCommitExecutor.runAfterCommit(...)`을 통해 트랜잭션이 커밋된 이후에만 `PortfolioFileUploader.delete(key)`를 실행한다 — DB 갱신이 롤백됐는데 S3 파일은 이미 지워지는 불일치를 방지하기 위함이다.
  - pgvector 청크 삭제(`PortfolioEmbeddingStore.deleteByPortfolioId`)는 같은 PostgreSQL 데이터소스를 쓰므로 DB 갱신과 같은 트랜잭션 안에서 처리한다. S3 삭제만 그 트랜잭션 커밋 후 별도로 실행한다 (전부-또는-전무 원칙 — pgvector 삭제가 실패하면 DB 갱신도 함께 롤백된다).
  - 소프트 삭제된 포트폴리오는 ID 기반 상태 조회·재삭제 API에서는 존재하지 않는 것으로 취급된다(404).
    목록 조회에서는 제외되며, 목록 API 자체는 200으로 반환된다. row 보존은 삭제·재업로드 제한 판정용이다.
  - 삭제(`existsDeletionSince` — `deleted=true` AND `status=READY` AND `deleted_at >= 이번달 1일`)와 재업로드(`existsReplacementSince` — `is_replacement=true` AND `status=READY` AND `uploaded_at >= 이번달 1일`)는 서로 다른 컬럼·조건을 보는 **독립된 월 1회 제한**이다. 한쪽을 이번 달에 이미 썼더라도 다른 쪽 기회는 남아있을 수 있다.
  - 두 제한 모두 **`READY`까지 간 포트폴리오만 실재하는 것으로 집계**한다. 업로드 진행 중 취소(`CANCELLED`)·처리 실패(`FAILED_FILE`·`FAILED_SYSTEM`) 건은 어느 쪽 기회도 소진하지 않으며, 기회가 이미 소진된 상태에서도 언제든 삭제할 수 있다(`Portfolio.countsTowardDeletionLimit()`). 소진된 삭제 기회 때문에 실패한 포트폴리오를 지우지도 새로 올리지도 못하고 다음 달까지 묶이는 상황을 막기 위함이다.
  - 같은 기준으로, 다음 업로드가 최초인지 재업로드인지도 `READY` 이력 유무로 판정한다(`existsCompletedByUserId`). 취소·실패 이력만 있는 유저의 다음 업로드는 여전히 최초 업로드로 취급되어 재업로드 기회를 소진하지 않는다.
- 열람: `GET /api/v1/portfolios/{portfolioId}/file-url` — `PortfolioFileUrlQueryService`가 소유권·`READY` 상태를 확인한 뒤 `PortfolioFileUploader.presignDownload(key)`로 GET presigned URL(유효시간 10분)을 발급한다. 면접 영상 재생(§3.3)과 동일하게 서버는 파일 바이트를 직접 다루지 않고 URL만 반환한다.

## 5. 리포트 없이 끝난 세션의 S3 잔여물 정리 배치

`interview_video` row는 `InterviewReportPersister.persist()`의 `insertRetentionIfAbsent`에서만 insert된다.
즉 **리포트 저장에 성공한 세션만** 3장의 만료·삭제 추적 대상이 되고, 그 외 세션이 남긴
`users/{userId}/sessions/{sessionId}/` 하위 파일은 어떤 테이블에도 추적되지 않는다.
`InterviewSessionFileCleanupScheduler`(매일 04:10 KST)가 이 잔여물을 회수한다.

### 5.1 정리 대상

리포트 생성 자체를 트리거하지 않는 종료 세션 중 `interview_video` row가 없고, `files_cleaned_at`이 비어 있는 건.

| 상태 | 리포트를 만들지 않는 이유 |
|---|---|
| `ABANDONED` (`abandon_cause != USER_EXIT`) | `InterviewAbandonPersister`가 이용권만 release하고 끝낸다 |
| `INVALID` | STT 실패율 30% 초과로 세션 무효화(`InterviewSttResetPersister`) |
| `PRELOAD_FAILED` | `InterviewPreloadFailureHandler`가 question·candidate row는 지우지만 이미 업로드된 질문 TTS는 S3에 남는다 |

제외 대상:

- `COMPLETED`·`ABANDONED(USER_EXIT)`인데 리포트 생성이 실패한 세션(`InterviewReportFailureHandler` 경로) —
  row는 없지만 리포트 재생성 여지가 있으므로 건드리지 않는다.
- `INSUFFICIENT_ANALYSIS` 리포트 — `persist()`를 타므로 row가 생성돼 이미 3장의 추적 대상이다.
- 아직 종료되지 않은 세션(`PREPARING`·`IN_PROGRESS`) — 위 세 상태에 해당하지 않아 조회에서 빠진다.

### 5.2 유예 시간

종료 후 **1시간**이 지난 세션만 대상으로 삼는다(`COALESCE(ended_at, created_at) < NOW() - 1h`).
세션이 끝난 뒤에도 답변 음성 비동기 저장, 지연된 preload TTS, 이미 발급된 presigned PUT URL(§2, 10분)로
파일이 뒤늦게 도착할 수 있어 그 창구가 모두 닫힌 뒤에 지운다.
`PRELOAD_FAILED`는 `ended_at`을 남기지 않으므로 `created_at`으로 대체해 판정한다.

### 5.3 삭제 범위와 재실행

- 세션 프리픽스(`users/{userId}/sessions/{sessionId}/`) 하위 전체를 지운다. 답변 음성·녹화본도 개인정보이므로
  `composite/`만 남기지 않는다. S3에 폴더 단위 삭제 API가 없어 `ListObjectsV2` + `DeleteObjects`로 처리한다.
- 삭제에 성공한 세션만 `interview_session.files_cleaned_at`을 기록해 다음 실행의 재조회 대상에서 뺀다.
  실패한 세션은 마킹하지 않아 다음 실행에서 자동으로 재시도되고, 한 세션이 실패해도 나머지 세션 정리는 계속된다.
- 한 번 실행당 500건 상한. 첫 실행 시 과거 누적분이 한꺼번에 몰리지 않게 하고, 남은 건은 다음 날 이어서 처리한다.
- 단일 인스턴스 전제로 분산 락을 두지 않는다. 다중 인스턴스가 되면 중복 실행이 가능하지만 S3 삭제는 멱등이다.

> 3장의 만료 영상 삭제 배치(§3.5)와는 별개다. 이 배치는 "추적 주체가 아예 없는" 세션을 다루고,
> §3.5는 "추적되고 있으나 만료된" 영상을 다룬다. §3.5는 여전히 미구현이다.
