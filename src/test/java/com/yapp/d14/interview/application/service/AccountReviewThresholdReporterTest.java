package com.yapp.d14.interview.application.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AccountReviewThresholdReporterTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @InjectMocks
    private AccountReviewThresholdReporter reporter;

    private final UUID userId = UUID.randomUUID();

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(AccountReviewThresholdReporter.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    private void givenDisconnectCount(long count) {
        given(interviewSessionRepository.countByUserIdAndAbandonCause(userId, AbandonCause.NETWORK_DISCONNECT))
                .willReturn(count);
    }

    private List<ILoggingEvent> warnEvents() {
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .toList();
    }

    @Test
    void 누적_15회에_도달하면_운영_검토_대상으로_경고한다() {
        givenDisconnectCount(15);

        reporter.reportIfThresholdReached(userId);

        assertThat(warnEvents()).hasSize(1);
        assertThat(warnEvents().getFirst().getFormattedMessage())
                .contains("[ACCOUNT REVIEW]")
                .contains(userId.toString());
    }

    @Test
    void 누적_14회면_아직_검토_대상이_아니다() {
        givenDisconnectCount(14);

        reporter.reportIfThresholdReached(userId);

        assertThat(warnEvents()).isEmpty();
    }

    @Test
    void 임계를_넘긴_뒤에는_다시_알리지_않는다() {
        givenDisconnectCount(16);

        reporter.reportIfThresholdReached(userId);

        assertThat(warnEvents()).isEmpty();
    }
}
