package com.yapp.d14.appversion.adapter.out.persistence;

import com.yapp.d14.appversion.adapter.out.persistence.entity.AppVersionPolicyJpaEntity;
import com.yapp.d14.appversion.application.port.out.AppVersionPolicyRepository;
import com.yapp.d14.appversion.domain.AppVersionPolicy;
import com.yapp.d14.appversion.domain.Platform;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class AppVersionPolicyPersistenceAdapter implements AppVersionPolicyRepository {

    private final AppVersionPolicyJpaRepository appVersionPolicyJpaRepository;

    @Override
    public Optional<AppVersionPolicy> findByPlatform(Platform platform) {
        return appVersionPolicyJpaRepository.findById(platform)
                .map(AppVersionPolicyJpaEntity::toDomain);
    }
}
