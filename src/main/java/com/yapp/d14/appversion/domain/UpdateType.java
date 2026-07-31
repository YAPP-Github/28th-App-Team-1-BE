package com.yapp.d14.appversion.domain;

public enum UpdateType {

    /** 현재 버전 &lt; 최소 지원 버전 — 강제 업데이트 */
    FORCE,
    /** 최소 지원 버전 ≤ 현재 버전 &lt; 최신 버전 — 권장 업데이트 */
    OPTIONAL,
    /** 현재 버전 ≥ 최신 버전 — 업데이트 불필요 */
    NONE
}
