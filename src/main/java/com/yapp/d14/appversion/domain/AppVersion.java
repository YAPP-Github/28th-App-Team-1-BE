package com.yapp.d14.appversion.domain;

import com.yapp.d14.appversion.exception.AppVersionErrorCode;
import com.yapp.d14.appversion.exception.AppVersionException;

import java.util.ArrayList;
import java.util.List;

// 자리수가 3자리로 보장되지 않으므로(1.2, 1.2.0.3 등) 문자열 비교 대신 자리별 정수로 비교한다(1.2 == 1.2.0).
public final class AppVersion implements Comparable<AppVersion> {

    private final String raw;
    private final List<Integer> segments;

    private AppVersion(String raw, List<Integer> segments) {
        this.raw = raw;
        this.segments = segments;
    }

    public static AppVersion parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AppVersionException(AppVersionErrorCode.INVALID_VERSION_FORMAT);
        }
        String trimmed = raw.trim();
        String[] parts = trimmed.split("\\.");
        List<Integer> segments = new ArrayList<>(parts.length);
        for (String part : parts) {
            // ASCII 숫자만 허용해 부호(+/-)·유니코드 숫자를 걸러낸다(parseInt는 둘 다 통과시킴).
            if (!isAsciiDigits(part)) {
                throw new AppVersionException(AppVersionErrorCode.INVALID_VERSION_FORMAT);
            }
            try {
                segments.add(Integer.parseInt(part));
            } catch (NumberFormatException e) {
                // 자리값이 int 범위를 넘는 경우
                throw new AppVersionException(AppVersionErrorCode.INVALID_VERSION_FORMAT);
            }
        }
        return new AppVersion(trimmed, segments);
    }

    private static boolean isAsciiDigits(String part) {
        if (part.isEmpty()) {
            return false;
        }
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
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
