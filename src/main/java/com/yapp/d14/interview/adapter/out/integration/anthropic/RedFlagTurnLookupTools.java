package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.application.port.out.RedFlagReconcileContext.Turn;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Optional;

class RedFlagTurnLookupTools {

    private final List<Turn> turns;

    RedFlagTurnLookupTools(List<Turn> turns) {
        this.turns = turns;
    }

    @Tool(description = "턴 번호로 해당 턴의 질문·답변 원문과 발화 구간을 조회한다. 레드플래그 후보를 확정하기 전 세부 내용을 검증할 때 사용한다.")
    String getTurnDetail(@ToolParam(description = "조회할 턴 번호") int turnNumber) {
        Optional<Turn> found = turns.stream()
                .filter(turn -> turn.turnNumber() == turnNumber)
                .findFirst();

        if (found.isEmpty()) {
            return "해당 턴 번호를 찾을 수 없습니다: " + turnNumber;
        }

        Turn turn = found.get();
        return """
                turn %d (axis: %s)
                Q: %s
                A: %s
                (start=%s, end=%s)
                """.formatted(
                turn.turnNumber(),
                turn.testType().name().toLowerCase(),
                turn.questionContent(),
                turn.skipped() ? "(스킵됨)" : turn.answerText(),
                turn.answerStartSec(),
                turn.answerEndSec()
        );
    }
}
