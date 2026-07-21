package com.yapp.d14.auth.application.service;

import com.yapp.d14.auth.application.port.out.SocialUserInfo;
import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.domain.Provider;
import com.yapp.d14.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SocialUserProvisionService {

    private final UserRepository userRepository;

    @Transactional
    public User provision(Provider provider, SocialUserInfo userInfo) {
        return userRepository.findByProviderAndProviderId(provider, userInfo.providerId())
                .orElseGet(() -> userRepository.save(
                        User.create(userInfo.email(), provider, userInfo.providerId())
                ));
    }
}
