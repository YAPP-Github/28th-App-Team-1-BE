package com.yapp.d14.portfolio.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioPiiMaskerTest {

    @Test
    void 이메일을_플레이스홀더로_치환한다() {
        PortfolioPiiMasker.MaskingResult result = PortfolioPiiMasker.mask("연락처: hong.gil-dong@gmail.com 입니다.");

        assertThat(result.maskedText()).isEqualTo("연락처: [이메일] 입니다.");
        assertThat(result.emailCount()).isEqualTo(1);
    }

    @Test
    void 휴대전화_번호를_구분자_형태와_무관하게_치환한다() {
        PortfolioPiiMasker.MaskingResult result =
                PortfolioPiiMasker.mask("010-1234-5678 / 01012345678 / 010 1234 5678");

        assertThat(result.maskedText()).isEqualTo("[전화번호] / [전화번호] / [전화번호]");
        assertThat(result.phoneCount()).isEqualTo(3);
    }

    @Test
    void 지역번호_유선전화를_치환한다() {
        PortfolioPiiMasker.MaskingResult result = PortfolioPiiMasker.mask("사무실 02-123-4567, 자택 031-1234-5678");

        assertThat(result.maskedText()).isEqualTo("사무실 [전화번호], 자택 [전화번호]");
        assertThat(result.phoneCount()).isEqualTo(2);
    }

    @Test
    void 체크섬을_통과한_주민등록번호만_치환한다() {
        PortfolioPiiMasker.MaskingResult result =
                PortfolioPiiMasker.mask("유효 900101-1234568 / 무효 900101-1234567");

        assertThat(result.maskedText()).isEqualTo("유효 [주민등록번호] / 무효 900101-1234567");
        assertThat(result.rrnCount()).isEqualTo(1);
    }

    @Test
    void 이메일_안에_포함된_숫자열이_전화번호로_먼저_잘리지_않는다() {
        PortfolioPiiMasker.MaskingResult result = PortfolioPiiMasker.mask("hong01012345678@naver.com");

        assertThat(result.maskedText()).isEqualTo("[이메일]");
        assertThat(result.emailCount()).isEqualTo(1);
        assertThat(result.phoneCount()).isZero();
    }

    @Test
    void 날짜와_버전_표기는_마스킹하지_않는다() {
        String text = "프로젝트 기간 2023.03.15 ~ 2024.01.20, 릴리스 v1.2.3, 결제 모듈 계좌 이체 기능 구현";

        PortfolioPiiMasker.MaskingResult result = PortfolioPiiMasker.mask(text);

        assertThat(result.maskedText()).isEqualTo(text);
        assertThat(result.phoneCount()).isZero();
        assertThat(result.rrnCount()).isZero();
    }

    @Test
    void 마스킹_대상이_없으면_원본을_그대로_반환한다() {
        String text = "Spring Boot 기반 백엔드 서비스를 설계하고 운영했습니다.";

        PortfolioPiiMasker.MaskingResult result = PortfolioPiiMasker.mask(text);

        assertThat(result.maskedText()).isEqualTo(text);
        assertThat(result.emailCount()).isZero();
        assertThat(result.phoneCount()).isZero();
        assertThat(result.rrnCount()).isZero();
    }

    @Test
    void 마스킹_건수를_유형별로_집계한다() {
        PortfolioPiiMasker.MaskingResult result = PortfolioPiiMasker.mask(
                "hong@gmail.com, dev@example.co.kr, 010-1234-5678"
        );

        assertThat(result.emailCount()).isEqualTo(2);
        assertThat(result.phoneCount()).isEqualTo(1);
        assertThat(result.rrnCount()).isZero();
    }
}
