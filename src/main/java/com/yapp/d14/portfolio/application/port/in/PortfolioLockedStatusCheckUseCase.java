package com.yapp.d14.portfolio.application.port.in;

import java.util.UUID;

public interface PortfolioLockedStatusCheckUseCase {

    /**
     * 포트폴리오 단위 어드바이저리 락을 잡은 뒤 소유권·READY 상태를 재검증한다.
     * 호출자(면접 세션 생성 저장 트랜잭션)와 삭제 트랜잭션이 같은 락 키로 직렬화되어,
     * "확인 시점엔 살아있었는데 그 사이 삭제된 포트폴리오"를 참조하는 세션이 생기는 경합을 막는다.
     * 반드시 호출자의 기존 트랜잭션 안에서 호출해야 락이 그 트랜잭션 끝까지 유지된다.
     */
    void requireReadyWithLock(UUID userId, UUID portfolioId);
}
