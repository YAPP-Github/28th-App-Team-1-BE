package com.yapp.d14.user.application.service;

import com.yapp.d14.user.application.port.in.UserNameDuplicateCheckUseCase;
import com.yapp.d14.user.application.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class UserNameDuplicateCheckService implements UserNameDuplicateCheckUseCase {

    private final UserRepository userRepository;

    @Override
    public boolean isAvailable(String name) {
        return !userRepository.existsByName(name);
    }
}
