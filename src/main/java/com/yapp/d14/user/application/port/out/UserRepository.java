package com.yapp.d14.user.application.port.out;

import com.yapp.d14.user.domain.Provider;
import com.yapp.d14.user.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findById(UUID id);

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    User save(User user);
}
