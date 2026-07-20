package com.yapp.d14.feedback.adapter.in.web.request;

import com.yapp.d14.feedback.application.command.GuestFeedbackSubmitCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GuestFeedbackSubmitHttpRequest(
        @Schema(description = "지인 별칭.", example = "재원")
        String nickname,

        @Schema(description = "항목별 척도 응답. 지정된 항목을 모두 채워야 제출 성립.")
        @NotEmpty @Valid List<Rating> ratings
) {

    public record Rating(
            @Schema(
                    description = "태도 항목",
                    example = "GAZE",
                    allowableValues = {"GAZE", "EXPRESSION", "POSTURE", "GESTURE", "VOICE"}
            )
            @NotBlank String axis,

            @Schema(description = "4단계 척도(1=좋았어요 ~ 4=아쉬웠어요)", example = "2", minimum = "1", maximum = "4")
            @NotNull Integer level,

            @Schema(description = "'왜 그렇게 느꼈나요?' 코멘트. 선택 입력.", example = "가끔 시선을 피하는 느낌이었어요.")
            String comment
    ) {
    }

    public GuestFeedbackSubmitCommand toCommand(String token, String deviceId) {
        List<GuestFeedbackSubmitCommand.RawRating> rawRatings = ratings.stream()
                .map(r -> new GuestFeedbackSubmitCommand.RawRating(r.axis(), r.level(), r.comment()))
                .toList();
        return GuestFeedbackSubmitCommand.of(token, deviceId, nickname, rawRatings);
    }
}
