package com.yapp.d14.user.application.service;

import com.yapp.d14.user.application.command.UserProfileUpdateCommand;
import com.yapp.d14.user.application.port.in.UserProfileUpdateUseCase;
import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.domain.User;
import com.yapp.d14.user.exception.UserErrorCode;
import com.yapp.d14.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class UserProfileUpdateService implements UserProfileUpdateUseCase {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void update(UserProfileUpdateCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (command.name() != null) {
            if (userRepository.existsByNameAndIdNot(command.name(), command.userId())) {
                throw new UserException(UserErrorCode.NAME_ALREADY_TAKEN);
            }
            user.registerName(command.name());
        }

        user.updateProfile(command.jobRole(), command.careerYears());

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new UserException(UserErrorCode.NAME_ALREADY_TAKEN);
        }
    }
}
