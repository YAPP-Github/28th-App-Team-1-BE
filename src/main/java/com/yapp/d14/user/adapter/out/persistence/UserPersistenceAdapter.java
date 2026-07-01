package com.yapp.d14.user.adapter.out.persistence;

import com.yapp.d14.user.adapter.out.persistence.entity.UserJpaEntity;
import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.domain.Provider;
import com.yapp.d14.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findByProviderAndProviderId(Provider provider, String providerId) {
        return userJpaRepository.findByProviderAndProviderId(provider, providerId)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public User save(User user) {
        return userJpaRepository.save(UserJpaEntity.from(user)).toDomain();
    }
}
