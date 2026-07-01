package com.yapp.d14.user.application.port.out;

import com.yapp.d14.user.domain.Provider;
import com.yapp.d14.user.domain.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    User save(User user);
}
