package com.yapp.d14.feedback.application.service;

import com.yapp.d14.feedback.application.port.in.GuestFeedbackReportQueryUseCase;
import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackReportView;
import com.yapp.d14.feedback.application.port.out.GuestFeedbackRepository;
import com.yapp.d14.feedback.domain.GuestFeedback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
class GuestFeedbackReportQueryService implements GuestFeedbackReportQueryUseCase {

    private final GuestFeedbackRepository guestFeedbackRepository;

    @Override
    @Transactional(readOnly = true)
    public GuestFeedbackReportView getForReport(Long sessionId) {
        List<GuestFeedbackReportView.Guest> guests = guestFeedbackRepository.findAllBySessionId(sessionId).stream()
                .map(GuestFeedbackReportQueryService::toGuest)
                .toList();
        return new GuestFeedbackReportView(guests.size(), guests);
    }

    private static GuestFeedbackReportView.Guest toGuest(GuestFeedback feedback) {
        List<GuestFeedbackReportView.Rating> ratings = feedback.getRatings().stream()
                .map(rating -> new GuestFeedbackReportView.Rating(rating.axis().name(), rating.level(), rating.comment()))
                .toList();
        return new GuestFeedbackReportView.Guest(feedback.getNickname(), ratings);
    }
}
