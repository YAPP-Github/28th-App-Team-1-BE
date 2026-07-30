package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.application.port.out.PriorQaCache;
import com.yapp.d14.interview.application.port.out.PriorTurn;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.portfolio.application.port.in.PortfolioChunkSearchUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioChunkResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
class LiveTurnTools {

    private static final int SEARCH_PORTFOLIO_TOP_K = 3;
    private static final int READ_PROJECT_DETAIL_TOP_K = 8;

    private final PortfolioChunkSearchUseCase portfolioChunkSearchUseCase;
    private final PriorQaCache priorQaCache;
    private final Long sessionId;
    private final UUID portfolioId;

    LiveTurnTools(
            PortfolioChunkSearchUseCase portfolioChunkSearchUseCase,
            PriorQaCache priorQaCache,
            Long sessionId,
            UUID portfolioId
    ) {
        this.portfolioChunkSearchUseCase = portfolioChunkSearchUseCase;
        this.priorQaCache = priorQaCache;
        this.sessionId = sessionId;
        this.portfolioId = portfolioId;
    }

    @Tool(name = "search_portfolio", description =
            "답변에 언급된 키워드로 포트폴리오에서 관련 내용을 검색해 텍스트 스니펫 목록을 반환한다.")
    List<String> searchPortfolio(@ToolParam(description = "검색 키워드") String keyword) {
        Instant startedAt = Instant.now();
        List<String> result = portfolioChunkSearchUseCase.searchChunks(portfolioId, keyword, SEARCH_PORTFOLIO_TOP_K).stream()
                .map(PortfolioChunkResult::text)
                .toList();
        log.info("[LIVE TURN TOOL] sessionId={}, tool=search_portfolio, keyword={}, resultCount={}, elapsedSeconds={}",
                sessionId, keyword, result.size(), elapsedSeconds(startedAt));
        return result;
    }

    @Tool(name = "read_project_detail", description =
            "특정 주제/프로젝트에 대해 search_portfolio보다 더 폭넓은 포트폴리오 텍스트를 조회한다(모순 대조 등 근거 보강용).")
    List<String> readProjectDetail(@ToolParam(description = "조회하고 싶은 프로젝트/주제 설명") String query) {
        Instant startedAt = Instant.now();
        List<String> result = portfolioChunkSearchUseCase.searchChunks(portfolioId, query, READ_PROJECT_DETAIL_TOP_K).stream()
                .map(PortfolioChunkResult::text)
                .toList();
        log.info("[LIVE TURN TOOL] sessionId={}, tool=read_project_detail, query={}, resultCount={}, elapsedSeconds={}",
                sessionId, query, result.size(), elapsedSeconds(startedAt));
        return result;
    }

    @Tool(name = "get_prior_qa", description =
            "current_axis가 아닌 다른 axis의 이전 턴 이력을 조회한다. 교차 axis 모순이 의심될 때만 사용한다.")
    List<PriorTurn> getPriorQa(
            @ToolParam(description = "조회할 axis: depth/boundary/connection/tradeoff/conflict/resilience") String axis
    ) {
        Instant startedAt = Instant.now();
        List<PriorTurn> result = priorQaCache.get(sessionId, TestType.valueOf(axis.toUpperCase()));
        log.info("[LIVE TURN TOOL] sessionId={}, tool=get_prior_qa, axis={}, resultCount={}, elapsedSeconds={}",
                sessionId, axis, result.size(), elapsedSeconds(startedAt));
        return result;
    }

    private double elapsedSeconds(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis() / 1000.0;
    }
}
