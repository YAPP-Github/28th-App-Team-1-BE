package com.yapp.d14.portfolio.application.port.out;

import com.yapp.d14.portfolio.domain.Portfolio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository {

    Portfolio save(Portfolio portfolio);

    // 유저 단위로 등록 트랜잭션을 직렬화하는 DB 어드바이저리 락(트랜잭션 종료 시 자동 해제).
    // 신규 등록은 아직 존재하지 않는 row에 대한 제약(활성 1건·월 1회 재업로드)을 검사해야 해서
    // 기존 row를 잠그는 SELECT ... FOR UPDATE로는 막을 수 없는 경합(phantom)이 생긴다.
    void acquireRegistrationLock(UUID userId);

    boolean existsActiveByUserId(UUID userId);

    boolean existsAnyByUserId(UUID userId);

    boolean existsReplacementSince(UUID userId, LocalDateTime since);

    Optional<Portfolio> findById(UUID id);

    List<Portfolio> findAllActiveByUserId(UUID userId);
}
