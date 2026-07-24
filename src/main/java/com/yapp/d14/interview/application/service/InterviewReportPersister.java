package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.AxisEvaluationRepository;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.application.port.out.RedFlagRepository;
import com.yapp.d14.interview.application.port.out.ReportCardRepository;
import com.yapp.d14.interview.application.port.out.ReportRepository;
import com.yapp.d14.interview.domain.AxisEvaluation;
import com.yapp.d14.interview.domain.InterviewVideo;
import com.yapp.d14.interview.domain.RedFlag;
import com.yapp.d14.interview.domain.Report;
import com.yapp.d14.interview.domain.ReportCard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
class InterviewReportPersister {

    private final ReportRepository reportRepository;
    private final AxisEvaluationRepository axisEvaluationRepository;
    private final RedFlagRepository redFlagRepository;
    private final ReportCardRepository reportCardRepository;
    private final InterviewVideoRepository interviewVideoRepository;

    @Transactional
    void persist(
            Long sessionId,
            Report report,
            List<AxisEvaluation> axisEvaluations,
            List<RedFlag> redFlags,
            List<ReportCard> reportCards
    ) {
        reportRepository.deleteBySessionId(sessionId);
        axisEvaluationRepository.deleteBySessionId(sessionId);
        redFlagRepository.deleteBySessionId(sessionId);
        reportCardRepository.deleteBySessionId(sessionId);

        reportRepository.save(report);
        axisEvaluationRepository.saveAll(axisEvaluations);
        redFlagRepository.saveAll(redFlags);
        reportCardRepository.saveAll(reportCards);

        ensureVideoRetentionStarted(sessionId);
    }

    // 분석 부족 리포트도 R1에 도달하므로(지인 축은 AI 분석 성패와 무관하게 성립) Step1을 동일하게 적용한다.
    // 이미 있으면(재생성·업로드 완료 선행) baseAt을 건드리지 않도록 DB upsert(ON CONFLICT DO NOTHING)로 넣는다.
    private void ensureVideoRetentionStarted(Long sessionId) {
        interviewVideoRepository.insertRetentionIfAbsent(InterviewVideo.create(sessionId, LocalDateTime.now()));
    }
}
