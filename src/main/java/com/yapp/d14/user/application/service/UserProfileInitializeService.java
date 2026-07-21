package com.yapp.d14.user.application.service;

import com.yapp.d14.job.domain.Job;
import com.yapp.d14.user.application.port.in.UserProfileInitializeUseCase;
import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
class UserProfileInitializeService implements UserProfileInitializeUseCase {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void initializeIfAbsent(UUID userId, String rawJobRole, Integer careerYears) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("[USER PROFILE INIT] 존재하지 않는 사용자라 프로필 동기화를 건너뜁니다: userId={}", userId);
            return;
        }

        if (user.getJobRole() != null && user.getCareerYears() != null) {
            return;
        }

        Job resolvedJobRole = user.getJobRole() != null ? user.getJobRole() : parseJobOrNull(rawJobRole, userId);
        Integer resolvedCareerYears = user.getCareerYears() != null ? user.getCareerYears() : careerYears;

        user.updateProfile(resolvedJobRole, resolvedCareerYears);
        userRepository.save(user);
    }

    private Job parseJobOrNull(String rawJobRole, UUID userId) {
        try {
            return Job.valueOf(rawJobRole);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("[USER PROFILE INIT] jobRole 매핑 실패, 해당 필드는 비워둡니다: userId={}, rawJobRole={}", userId, rawJobRole);
            return null;
        }
    }
}
