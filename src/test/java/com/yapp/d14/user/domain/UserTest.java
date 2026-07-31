package com.yapp.d14.user.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private User newUser() {
        return User.create("a@a.com", Provider.KAKAO, "pid");
    }

    @Test
    void 이름_없이_직무_연차만_설정하면_profileRegistered는_false다() {
        User user = newUser();

        user.updateProfile(JobRole.BACKEND, 3);

        assertThat(user.isProfileRegistered()).isFalse();
    }

    @Test
    void 이름만_등록하고_직무_연차가_없으면_profileRegistered는_false다() {
        User user = newUser();

        user.registerName("홍길동");

        assertThat(user.isProfileRegistered()).isFalse();
    }

    @Test
    void 이름_직무_연차가_모두_있으면_profileRegistered는_true다() {
        User user = newUser();

        user.registerName("홍길동");
        user.updateProfile(JobRole.BACKEND, 3);

        assertThat(user.isProfileRegistered()).isTrue();
    }

    @Test
    void 직무_연차를_먼저_등록하고_이름을_나중에_등록해도_profileRegistered는_true가_된다() {
        User user = newUser();

        user.updateProfile(JobRole.BACKEND, 3);
        user.registerName("홍길동");

        assertThat(user.isProfileRegistered()).isTrue();
    }
}
