package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionStatusUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionPollStatus;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionStatusResult;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionStatusResult.SummaryQuestion;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.InterviewVoiceStorage;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class InterviewSessionStatusQueryService implements InterviewSessionStatusUseCase {

    private static final int SUMMARY_TURN_LEVEL = 0;

    private final InterviewSessionRepository interviewSessionRepository;
    private final QuestionRepository questionRepository;
    private final InterviewVoiceStorage interviewVoiceStorage;
    private final InterviewPreloadFailureHandler interviewPreloadFailureHandler;

    @Override
    @Transactional
    public InterviewSessionStatusResult getStatus(UUID userId, Long sessionId) {
        InterviewSession session = InterviewSessionAccessSupport.requireOwned(interviewSessionRepository, sessionId, userId);
        timeoutIfPreloadStale(session);

        InterviewSessionPollStatus pollStatus = InterviewSessionPollStatus.from(session.getStatus());
        if (pollStatus != InterviewSessionPollStatus.READY) {
            return new InterviewSessionStatusResult(pollStatus, null, null);
        }

        SummaryQuestion summaryQuestion = questionRepository
                .findBySessionIdAndTurnLevel(sessionId, SUMMARY_TURN_LEVEL)
                .map(this::toSummaryQuestion)
                .orElse(null);

        return new InterviewSessionStatusResult(InterviewSessionPollStatus.READY, session.getStartedAt(), summaryQuestion);
    }

    // markFailed()가 세션을 다시 조회해 실패 처리(후보/질문 삭제·이용권 release)까지 전담하므로,
    // 여기서는 이번 조회 응답에 곧바로 반영되도록 메모리상의 session 상태만 함께 맞춰준다.
    private void timeoutIfPreloadStale(InterviewSession session) {
        if (session.isPreloadTimedOut()) {
            interviewPreloadFailureHandler.markFailed(session.getId());
            session.markPreloadFailed();
        }
    }

    private SummaryQuestion toSummaryQuestion(Question question) {
        String ttsAudio = question.getAiVoiceS3Key() != null
                ? interviewVoiceStorage.readBase64(question.getAiVoiceS3Key())
                : null;

        return new SummaryQuestion(
                question.getId(),
                ttsAudio,
                question.getTurnLevel(),
                question.getDepthLevel()
        );
    }
}
