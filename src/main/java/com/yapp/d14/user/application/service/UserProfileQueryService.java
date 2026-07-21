package com.yapp.d14.user.application.service;

import com.yapp.d14.ticket.application.port.in.TicketRemainingQueryUseCase;
import com.yapp.d14.user.application.port.in.UserProfileQueryUseCase;
import com.yapp.d14.user.application.port.in.result.UserProfileResult;
import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.domain.User;
import com.yapp.d14.user.exception.UserErrorCode;
import com.yapp.d14.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class UserProfileQueryService implements UserProfileQueryUseCase {

    private final UserRepository userRepository;
    private final TicketRemainingQueryUseCase ticketRemainingQueryUseCase;

    @Override
    public UserProfileResult getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        int remainingTicketCount = ticketRemainingQueryUseCase.getRemaining(userId);

        return new UserProfileResult(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.isNameRegistered(),
                user.getJobRole(),
                user.getCareerYears(),
                remainingTicketCount
        );
    }
}
