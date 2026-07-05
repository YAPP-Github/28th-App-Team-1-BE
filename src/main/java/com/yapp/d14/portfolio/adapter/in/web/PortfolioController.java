package com.yapp.d14.portfolio.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.portfolio.adapter.in.web.request.PortfolioRegisterHttpRequest;
import com.yapp.d14.portfolio.adapter.in.web.response.PortfolioDeleteHttpResponse;
import com.yapp.d14.portfolio.adapter.in.web.response.PortfolioListHttpResponse;
import com.yapp.d14.portfolio.adapter.in.web.response.PortfolioRegisterHttpResponse;
import com.yapp.d14.portfolio.adapter.in.web.response.PortfolioStatusHttpResponse;
import com.yapp.d14.portfolio.application.port.in.PortfolioDeleteResult;
import com.yapp.d14.portfolio.application.port.in.PortfolioDeleteUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioListUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioRegisterResult;
import com.yapp.d14.portfolio.application.port.in.PortfolioRegisterUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioStatusResult;
import com.yapp.d14.portfolio.application.port.in.PortfolioStatusUseCase;
import com.yapp.d14.portfolio.application.port.in.PortfolioSummary;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
class PortfolioController implements PortfolioControllerDocs {

    private final PortfolioRegisterUseCase portfolioRegisterUseCase;
    private final PortfolioStatusUseCase portfolioStatusUseCase;
    private final PortfolioListUseCase portfolioListUseCase;
    private final PortfolioDeleteUseCase portfolioDeleteUseCase;

    @Override
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PortfolioRegisterHttpResponse>> register(
            @CurrentUser UUID userId,
            @RequestPart("file") MultipartFile file,
            @Valid @ModelAttribute PortfolioRegisterHttpRequest request
    ) {
        PortfolioRegisterResult result = portfolioRegisterUseCase.register(request.toCommand(userId, file));
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(PortfolioRegisterHttpResponse.from(result)));
    }

    @Override
    @GetMapping("/{portfolioId}/status")
    public ResponseEntity<ApiResponse<PortfolioStatusHttpResponse>> getStatus(
            @CurrentUser UUID userId,
            @PathVariable UUID portfolioId
    ) {
        PortfolioStatusResult result = portfolioStatusUseCase.getStatus(userId, portfolioId);
        return ResponseEntity.ok(ApiResponse.ok(PortfolioStatusHttpResponse.from(result)));
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<PortfolioListHttpResponse>> getList(@CurrentUser UUID userId) {
        List<PortfolioSummary> summaries = portfolioListUseCase.getList(userId);
        return ResponseEntity.ok(ApiResponse.ok(PortfolioListHttpResponse.from(summaries)));
    }

    @Override
    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<PortfolioDeleteHttpResponse>> delete(
            @CurrentUser UUID userId,
            @PathVariable UUID portfolioId
    ) {
        PortfolioDeleteResult result = portfolioDeleteUseCase.delete(userId, portfolioId);
        return ResponseEntity.ok(ApiResponse.ok(PortfolioDeleteHttpResponse.from(result)));
    }
}
