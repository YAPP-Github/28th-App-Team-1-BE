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
  │                 ├─ answers/            ← 사용자 답변 음성
  │                 │     └─ {turnId}.webm (or .wav)
  │                 │
  │                 ├─ questions/          ← AI 면접관 TTS 음성
  │                 │     └─ {turnId}.mp3
  │                 │
  │                 ├─ recording/          ← 면접 영상 (프론트 녹화본)
  │                 │     └─ raw.webm
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
- Presigned URL 자체의 서명 유효시간(TTL)은 아래 3장의 `video_expires_at`(비즈니스 만료 시점)과 **별개의 값**이다. 링크 자체는 짧게(예: 5~10분), 콘텐츠 접근 가능 기간은 별도 정책(3장)을 따른다.

## 3. 인터뷰 세션 영상 만료·삭제 정책

### 3.1 원칙

- S3에는 영상을 저장만 하고, S3 자체의 만료(Lifecycle) 설정은 사용하지 않는다.
- 실제 접근 가능 여부는 `report` 테이블의 `video_expires_at` 값으로 제어한다.
- 물리적 삭제는 별도 배치가 지연 처리한다 (접근 제어와 스토리지 정리를 분리).

### 3.2 만료 시점 연장 규칙 (기획에 따라 변경 가능성 있음)

| 트리거 | 계산식 |
|---|---|
| 기본 (세션 생성 시 1회) | 세션 시작 + 24h |
| 피드백 요청 전송 | `NOW()` + 48h |
| 지인 시청 | 세션 시작 + 7일 |
| 지인 2명 시청 완료 | `NOW()` + 30일 |


### 3.3 영상 URL 제공 흐름

```
video_expires_at > NOW() 확인
  ├─ 유효 → Presigned URL 생성 후 반환
  └─ 만료 → 영상 만료 응답
```

### 3.4 실제 삭제 배치 (매일 자정)

```
대상: video_expires_at < NOW() AND video_deleted = FALSE
동작: S3 파일 삭제 → video_deleted = TRUE 업데이트
```

- S3 key는 `userId` + `sessionId`로부터 결정적으로 계산 가능하므로, `report` 테이블에 별도 S3 key 컬럼 없이 배치에서 재계산해 삭제한다. (세션당 영상이 1개라는 전제 하에 유효)
- 삭제 대상 범위(⚠️ 확인 필요): `composite/final.mp4`만 삭제할지, `sessions/{sessionId}/` 하위 전체(`answers/`, `questions/`, `recording/`, `composite/`)를 함께 삭제할지 정해야 한다. 원본 음성·녹화본도 개인정보이므로 함께 정리하는 편이 자연스럽다.
- S3에 폴더 단위 삭제 API는 없으므로, 하위 전체를 지운다면 `ListObjectsV2` + `DeleteObjects`(배치 삭제)로 구현해야 한다. 단일 객체만 지운다면 `DeleteObject`로 충분하다.
- 배치는 최대 24h 지연될 수 있지만, 3.3의 API 레벨 체크(`video_expires_at > NOW()`)가 이미 접근을 차단하므로 사용자 노출 관점에서는 문제가 없다. 순수 스토리지 비용 정리 목적의 지연이다.

## 4. 포트폴리오 파일 생명주기

- 업로드: `POST /api/v1/portfolios` 등록 시 비동기로 S3 업로드 (`PortfolioProcessService`, `S3PortfolioFileUploaderAdapter`).
- 삭제: 포트폴리오 삭제는 **소프트 삭제**다. `portfolios` row는 재업로드 월 1회 제한 판정을 위한 이력으로 보존되고(`deleted=true`, `deleted_at` 기록), S3 원본 파일과 pgvector 임베딩은 이력 보존과 무관하게 즉시 물리 삭제된다. `PortfolioDeleteService.delete()`는 `@Transactional`로 DB 갱신(소프트 삭제 `save`)을 감싸고, `AfterCommitExecutor.runAfterCommit(...)`을 통해 트랜잭션이 커밋된 이후에만 `PortfolioFileUploader.delete(key)`를 실행한다 — DB 갱신이 롤백됐는데 S3 파일은 이미 지워지는 불일치를 방지하기 위함이다.
  - pgvector 청크 삭제(`PortfolioEmbeddingStore.deleteByPortfolioId`)는 같은 PostgreSQL 데이터소스를 쓰므로 DB 갱신과 같은 트랜잭션 안에서 처리한다. S3 삭제만 그 트랜잭션 커밋 후 별도로 실행한다 (전부-또는-전무 원칙 — pgvector 삭제가 실패하면 DB 갱신도 함께 롤백된다).
  - 소프트 삭제된 포트폴리오는 상태 조회·목록·재삭제 등 API에서는 존재하지 않는 것으로 취급된다(404). row 보존은 순수하게 재업로드 제한 판정용이다.
