package com.yapp.d14.user.application.service;

import com.yapp.d14.user.application.port.in.UserNameRegisterUseCase;
import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.domain.User;
import com.yapp.d14.user.exception.UserErrorCode;
import com.yapp.d14.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class UserNameRegisterService implements UserNameRegisterUseCase {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void register(UUID userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (userRepository.existsByNameAndIdNot(name, userId)) {
            throw new UserException(UserErrorCode.NAME_ALREADY_TAKEN);
        }

        user.registerName(name);

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new UserException(UserErrorCode.NAME_ALREADY_TAKEN);
        }
    }
}
