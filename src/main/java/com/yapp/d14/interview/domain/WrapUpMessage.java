package com.yapp.d14.interview.domain;

// 면접관 마무리 멘트(종료 TTS) 문구. 종료 유형별 고정 문구를 반환하고, 마무리 멘트가 없는 종료 유형(중도 이탈 등)은 null.
// 답변 제출 시 TTS 생성, 영상 합성, 리포트 대본이 같은 문구/유무 판단을 공유하도록 한곳에 둔다.
public final class WrapUpMessage {

    private static final String MANUAL_END = "오늘 면접은 여기까지 하겠습니다. 수고하셨습니다.";
    private static final String HARD_CAP = "면접 시간이 다 되어 곧 마무리하겠습니다. 잠시 후 종료됩니다.";
    private static final String NORMAL_END = "수고하셨습니다. 오늘 면접은 여기까지입니다.";

    private WrapUpMessage() {
    }

    /** 종료 유형에 해당하는 마무리 멘트 문구. 마무리 멘트가 없으면 null(예: BACK_EXIT). */
    public static String textFor(InterviewEndType endType) {
        if (endType == null) {
            return null;
        }
        return switch (endType) {
            case MANUAL_END -> MANUAL_END;
            case HARD_CAP -> HARD_CAP;
            case NORMAL_END -> NORMAL_END;
            default -> null;
        };
    }
}
