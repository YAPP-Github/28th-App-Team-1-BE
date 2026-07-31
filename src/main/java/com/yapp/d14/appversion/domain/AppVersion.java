package com.yapp.d14.appversion.domain;

import com.yapp.d14.appversion.exception.AppVersionErrorCode;
import com.yapp.d14.appversion.exception.AppVersionException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 마케팅 버전(SemVer, {@code x.x.x})을 자리별 정수로 비교하는 값 객체.
 *
 * <p>버전 형식이 항상 3자리로 보장되지 않으므로({@code 1.2}, {@code 1.2.0.3} 등) 문자열 단순 비교는 금지하고
 * 자리별 정수 비교만 수행한다. 비교 시 부족한 자리는 0으로 간주한다({@code 1.2} == {@code 1.2.0}).
 */
public final class AppVersion implements Comparable<AppVersion> {

    private static final Pattern NUMERIC = Pattern.compile("\\d+");

    private final String raw;
    private final List<Integer> segments;

    private AppVersion(String raw, List<Integer> segments) {
        this.raw = raw;
        this.segments = segments;
    }

    /**
     * 클라이언트/DB가 전달한 버전 문자열을 방어적으로 파싱한다.
     * 비어 있거나 숫자가 아닌 자리가 있으면 {@link AppVersionErrorCode#INVALID_VERSION_FORMAT} 예외를 던진다.
     */
    public static AppVersion parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AppVersionException(AppVersionErrorCode.INVALID_VERSION_FORMAT);
        }
        String trimmed = raw.trim();
        String[] parts = trimmed.split("\\.");
        List<Integer> segments = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (!NUMERIC.matcher(part).matches()) {
                throw new AppVersionException(AppVersionErrorCode.INVALID_VERSION_FORMAT);
            }
            try {
                segments.add(Integer.parseInt(part));
            } catch (NumberFormatException e) {
                throw new AppVersionException(AppVersionErrorCode.INVALID_VERSION_FORMAT);
            }
        }
        return new AppVersion(trimmed, segments);
    }

    @Override
    public int compareTo(AppVersion other) {
        int length = Math.max(segments.size(), other.segments.size());
        for (int i = 0; i < length; i++) {
            int a = i < segments.size() ? segments.get(i) : 0;
            int b = i < other.segments.size() ? other.segments.get(i) : 0;
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        return 0;
    }

    public boolean isLowerThan(AppVersion other) {
        return compareTo(other) < 0;
    }

    public String value() {
        return raw;
    }
}
