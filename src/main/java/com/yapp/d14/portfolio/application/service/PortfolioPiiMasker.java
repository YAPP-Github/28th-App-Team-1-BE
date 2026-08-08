package com.yapp.d14.portfolio.application.service;

import lombok.experimental.UtilityClass;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
class PortfolioPiiMasker {

    private static final Pattern EMAIL = Pattern.compile("\\b[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern MOBILE = Pattern.compile("\\b01[016789][-.\\s]?\\d{3,4}[-.\\s]?\\d{4}\\b");
    private static final Pattern LANDLINE = Pattern.compile("\\b0(?:2|[3-6][1-5])[-.\\s]?\\d{3,4}[-.\\s]?\\d{4}\\b");
    private static final Pattern RRN = Pattern.compile("\\b\\d{6}[-\\s]?\\d{7}\\b");

    private static final int[] RRN_WEIGHTS = {2, 3, 4, 5, 6, 7, 8, 9, 2, 3, 4, 5};
    private static final int RRN_DIGIT_LENGTH = 13;

    private static final String EMAIL_MASK = "[이메일]";
    private static final String PHONE_MASK = "[전화번호]";
    private static final String RRN_MASK = "[주민등록번호]";

    private static final int EMAIL_INDEX = 0;
    private static final int PHONE_INDEX = 1;
    private static final int RRN_INDEX = 2;

    MaskingResult mask(String rawText) {
        int[] counts = new int[3];

        // 이메일을 먼저 치환해야 hong01012345678@naver.com 같은 값의 로컬파트가 전화번호로 잘리지 않는다.
        String text = EMAIL.matcher(rawText).replaceAll(match -> {
            counts[EMAIL_INDEX]++;
            return EMAIL_MASK;
        });
        text = MOBILE.matcher(text).replaceAll(match -> {
            counts[PHONE_INDEX]++;
            return PHONE_MASK;
        });
        text = LANDLINE.matcher(text).replaceAll(match -> {
            counts[PHONE_INDEX]++;
            return PHONE_MASK;
        });
        text = RRN.matcher(text).replaceAll(match -> {
            if (!isValidRrn(match.group())) {
                return Matcher.quoteReplacement(match.group());
            }
            counts[RRN_INDEX]++;
            return RRN_MASK;
        });

        return new MaskingResult(text, counts[EMAIL_INDEX], counts[PHONE_INDEX], counts[RRN_INDEX]);
    }

    // 정규식만으로는 임의의 13자리 숫자까지 전부 걸리므로, 공개된 체크섬으로 한 번 더 걸러낸다.
    private boolean isValidRrn(String candidate) {
        String digits = candidate.replaceAll("\\D", "");
        if (digits.length() != RRN_DIGIT_LENGTH) {
            return false;
        }

        int total = 0;
        for (int i = 0; i < RRN_WEIGHTS.length; i++) {
            total += Character.getNumericValue(digits.charAt(i)) * RRN_WEIGHTS[i];
        }

        int checkDigit = (11 - (total % 11)) % 10;
        return checkDigit == Character.getNumericValue(digits.charAt(RRN_DIGIT_LENGTH - 1));
    }

    record MaskingResult(String maskedText, int emailCount, int phoneCount, int rrnCount) {
    }
}
