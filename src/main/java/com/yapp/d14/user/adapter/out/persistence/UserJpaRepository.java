package com.yapp.d14.user.adapter.out.persistence;

import com.yapp.d14.user.adapter.out.persistence.entity.UserJpaEntity;
import com.yapp.d14.user.domain.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByProviderAndProviderId(Provider provider, String providerId);
}
