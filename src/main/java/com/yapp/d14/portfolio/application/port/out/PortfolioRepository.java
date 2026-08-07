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

    // 포트폴리오 단위로 삭제와 "삭제 여부 확인 후 사용(면접 세션 생성)"을 직렬화하는 DB 어드바이저리 락.
    // 삭제(PortfolioDeleteService)와 세션 생성 시 재검증(InterviewSessionPersister)이 같은 키로 이 락을 잡아,
    // "확인 시점엔 살아있었는데 그 사이 삭제된 포트폴리오를 참조하는 세션이 생성되는" TOCTOU 경합을 막는다.
    void acquirePortfolioLock(UUID portfolioId);

    // 실재하는 포트폴리오(READY이고 삭제되지 않음)가 있는지 — 신규 등록을 막는 기준.
    // 실패(FAILED_FILE·FAILED_SYSTEM) 건은 실재하지 않는 것으로 보고 재업로드를 막지 않는다
    // (s3-policy.md §4의 "취소·실패 건은 어느 쪽 기회도 소진하지 않는다"와 같은 기준).
    boolean existsActiveByUserId(UUID userId);

    // 아직 분석 중인 포트폴리오가 있는지 — 동시에 두 건이 READY가 되는 것을 막기 위해 등록을 함께 차단한다.
    boolean existsProcessingByUserId(UUID userId);

    // 완료(READY)까지 간 포트폴리오를 가진 적이 있는지 — 다음 업로드가 최초인지 재업로드인지를 가른다.
    // 취소·실패 건만 남은 유저는 아직 최초 업로드로 취급한다.
    boolean existsCompletedByUserId(UUID userId);

    boolean existsReplacementSince(UUID userId, LocalDateTime since);

    boolean existsDeletionSince(UUID userId, LocalDateTime since);

    Optional<Portfolio> findById(UUID id);

    List<Portfolio> findAllActiveByUserId(UUID userId);
}
