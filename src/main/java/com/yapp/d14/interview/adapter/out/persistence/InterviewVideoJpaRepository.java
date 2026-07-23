package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.InterviewVideoJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    @Modifying
    @Query(value = """
            INSERT INTO interview_video (session_id, base_at, expires_at, deleted, uploaded)
            VALUES (:sessionId, :baseAt, :expiresAt, false, true)
            ON CONFLICT (session_id) DO UPDATE SET uploaded = true
            """, nativeQuery = true)
    void upsertUploaded(@Param("sessionId") Long sessionId,
                        @Param("baseAt") LocalDateTime baseAt,
                        @Param("expiresAt") LocalDateTime expiresAt);
}
