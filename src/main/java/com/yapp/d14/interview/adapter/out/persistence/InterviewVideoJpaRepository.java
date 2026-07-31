package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.InterviewVideoJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

interface InterviewVideoJpaRepository extends JpaRepository<InterviewVideoJpaEntity, Long> {

    Optional<InterviewVideoJpaEntity> findBySessionId(Long sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM InterviewVideoJpaEntity v WHERE v.sessionId = :sessionId")
    Optional<InterviewVideoJpaEntity> findBySessionIdForUpdate(@Param("sessionId") Long sessionId);

    // 보관 타이머를 없을 때만 생성한다. 리포트 채점과 업로드 완료가 동시에 최초 INSERT를 시도해도
    // DB가 unique(session_id) 충돌을 흡수하므로 예외가 나지 않는다. 이미 있으면 baseAt을 건드리지 않는다.
    @Modifying
    @Query(value = """
            INSERT INTO interview_video (session_id, base_at, expires_at, deleted, uploaded)
            VALUES (:sessionId, :baseAt, :expiresAt, false, false)
            ON CONFLICT (session_id) DO NOTHING
            """, nativeQuery = true)
    void insertRetentionIfAbsent(@Param("sessionId") Long sessionId,
                                 @Param("baseAt") LocalDateTime baseAt,
                                 @Param("expiresAt") LocalDateTime expiresAt);

    // 업로드 완료를 표시한다. 레코드가 없으면 보관 타이머와 함께 생성(uploaded=true)하고,
    // 있으면 uploaded만 true로 올린다. expires_at은 건드리지 않아 보관기간 연장과 충돌(Lost Update)하지 않는다.
    // 마무리 멘트 재생 구간(wrap_up_*)도 함께 쓴다. 멱등 재호출이 값을 지우지 않도록 COALESCE(신규값 우선, 없으면 기존값 유지).
    @Modifying
    @Query(value = """
            INSERT INTO interview_video (session_id, base_at, expires_at, deleted, uploaded, wrap_up_start_sec, wrap_up_end_sec)
            VALUES (:sessionId, :baseAt, :expiresAt, false, true, :wrapUpStartSec, :wrapUpEndSec)
            ON CONFLICT (session_id) DO UPDATE SET
                uploaded = true,
                wrap_up_start_sec = COALESCE(EXCLUDED.wrap_up_start_sec, interview_video.wrap_up_start_sec),
                wrap_up_end_sec = COALESCE(EXCLUDED.wrap_up_end_sec, interview_video.wrap_up_end_sec)
            """, nativeQuery = true)
    void upsertUploaded(@Param("sessionId") Long sessionId,
                        @Param("baseAt") LocalDateTime baseAt,
                        @Param("expiresAt") LocalDateTime expiresAt,
                        @Param("wrapUpStartSec") Float wrapUpStartSec,
                        @Param("wrapUpEndSec") Float wrapUpEndSec);

    // 합성 완료 표시. composited 한 컬럼만 갱신해 보관기간 연장·업로드 완료와 충돌(Lost Update)하지 않는다.
    // 합성(ffmpeg)은 트랜잭션 밖 비동기로 돌기 때문에, 이 단건 UPDATE는 자체 트랜잭션으로 커밋한다.
    @Modifying
    @Transactional
    @Query("UPDATE InterviewVideoJpaEntity v SET v.composited = true WHERE v.sessionId = :sessionId")
    int markComposited(@Param("sessionId") Long sessionId);
}
