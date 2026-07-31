package com.yapp.d14.ticket.adapter.out.persistence;

import com.yapp.d14.ticket.adapter.out.persistence.entity.UserTicketJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

interface UserTicketJpaRepository extends JpaRepository<UserTicketJpaEntity, UUID> {

    // 동시 요청이 최초 이용권 초기화를 함께 시도해도 DB가 unique(user_id) 충돌을 흡수하므로 예외가 나지 않는다.
    @Modifying
    @Query(value = """
            INSERT INTO user_ticket (user_id, remaining, updated_at)
            VALUES (:userId, :remaining, :updatedAt)
            ON CONFLICT (user_id) DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(@Param("userId") UUID userId,
                        @Param("remaining") int remaining,
                        @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserTicketJpaEntity t SET t.remaining = t.remaining - 1, t.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE t.userId = :userId AND t.remaining > 0")
    int decrementIfAvailable(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserTicketJpaEntity t SET t.remaining = t.remaining + 1, t.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE t.userId = :userId")
    void increment(@Param("userId") UUID userId);
}
