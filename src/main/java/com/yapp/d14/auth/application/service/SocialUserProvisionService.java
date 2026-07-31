package com.yapp.d14.auth.application.service;

import com.yapp.d14.auth.application.port.out.SocialUserInfo;
import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.domain.Provider;
import com.yapp.d14.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class SocialUserProvisionService {

    private final UserRepository userRepository;

    @Transactional
    public UserProvisionResult provision(Provider provider, SocialUserInfo userInfo) {
        Optional<User> existingUser = userRepository.findByProviderAndProviderId(provider, userInfo.providerId());
        boolean newlyCreated = existingUser.isEmpty();
        User user = existingUser.orElseGet(
                () -> userRepository.save(User.create(userInfo.email(), provider, userInfo.providerId()))
        );

        if (provider == Provider.APPLE && userInfo.appleRefreshToken() != null) {
            user.updateAppleRefreshToken(userInfo.appleRefreshToken());
            user = userRepository.save(user);
        }

        return new UserProvisionResult(user, newlyCreated);
    }
}
