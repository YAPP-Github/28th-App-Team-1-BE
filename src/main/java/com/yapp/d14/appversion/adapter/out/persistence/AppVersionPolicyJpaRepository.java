package com.yapp.d14.appversion.adapter.out.persistence;

import com.yapp.d14.appversion.adapter.out.persistence.entity.AppVersionPolicyJpaEntity;
import com.yapp.d14.appversion.domain.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

interface AppVersionPolicyJpaRepository extends JpaRepository<AppVersionPolicyJpaEntity, Platform> {
}
