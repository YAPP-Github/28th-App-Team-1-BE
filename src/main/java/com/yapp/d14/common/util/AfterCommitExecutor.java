package com.yapp.d14.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AfterCommitExecutor {

    // 진행 중인 트랜잭션이 커밋된 이후에만 action을 실행한다.
    // 트랜잭션이 롤백되면 실행되지 않으므로, DB 롤백과 외부 자원(S3 등) 정리 사이의 불일치를 막는다.
    // 활성화된 트랜잭션이 없으면 즉시 실행한다.
    public static void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
