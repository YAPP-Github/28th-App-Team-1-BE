package com.yapp.d14.portfolio.adapter.in.web;

import com.yapp.d14.common.response.ApiResponse;
import com.yapp.d14.common.web.CurrentUser;
import com.yapp.d14.portfolio.adapter.in.web.request.PortfolioRegisterHttpRequest;
import com.yapp.d14.portfolio.adapter.in.web.response.PortfolioRegisterHttpResponse;
import com.yapp.d14.portfolio.application.port.in.PortfolioRegisterResult;
import com.yapp.d14.portfolio.application.port.in.PortfolioRegisterUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
class PortfolioController implements PortfolioControllerDocs {

    private final PortfolioRegisterUseCase portfolioRegisterUseCase;

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
}
