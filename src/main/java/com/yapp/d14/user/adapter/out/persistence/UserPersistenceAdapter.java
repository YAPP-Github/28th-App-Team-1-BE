package com.yapp.d14.user.adapter.out.persistence;

import com.yapp.d14.user.adapter.out.persistence.entity.UserJpaEntity;
import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.domain.Provider;
import com.yapp.d14.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByProviderAndProviderId(Provider provider, String providerId) {
        return userJpaRepository.findByProviderAndProviderId(provider, providerId)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return userJpaRepository.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, UUID id) {
        return userJpaRepository.existsByNameAndIdNot(name, id);
    }

    @Override
    public User save(User user) {
        return userJpaRepository.saveAndFlush(UserJpaEntity.from(user)).toDomain();
    }

    @Override
    public void deleteById(UUID id) {
        // 서비스단에서 이미 존재를 확인했으므로, deleteById() 내부의 findById 재조회를 피하기 위해
        // getReferenceById(프록시)로 select 없이 삭제한다.
        userJpaRepository.delete(userJpaRepository.getReferenceById(id));
    }
}
