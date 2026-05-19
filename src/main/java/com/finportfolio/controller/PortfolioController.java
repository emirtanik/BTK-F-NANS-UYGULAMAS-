package com.finportfolio.controller;

import com.finportfolio.exception.BusinessException;
import com.finportfolio.dto.PortfolioItemRequest;
import com.finportfolio.dto.PortfolioItemResponse;
import com.finportfolio.dto.PortfolioSummaryResponse;
import com.finportfolio.security.JwtService;
import com.finportfolio.service.PortfolioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "3. Portföy", description = "Yatırım yönetimi ve portföy analizi")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final JwtService jwtService;
    private final com.finportfolio.service.PortfolioAnalysisService portfolioAnalysisService;
    
    @GetMapping
    public ResponseEntity<PortfolioSummaryResponse> getPortfolio(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(portfolioService.getPortfolio(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<PortfolioItemResponse> addItem(
            @Valid @RequestBody PortfolioItemRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = extractUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(portfolioService.addItem(userId, request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<PortfolioItemResponse> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody PortfolioItemRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = extractUserId(httpRequest);
        return ResponseEntity.ok(portfolioService.updateItem(userId, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long itemId,
            HttpServletRequest httpRequest
    ) {
        Long userId = extractUserId(httpRequest);
        portfolioService.deleteItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return jwtService.extractUserId(authHeader.substring(7));
        }
        throw new BusinessException("Authorization header eksik", HttpStatus.UNAUTHORIZED);
    }
    /**
     * Portfoy analizi: risk skoru, cesitlendirme, oneriler.
     */
    @GetMapping("/analysis")
    public ResponseEntity<com.finportfolio.dto.PortfolioAnalysisResponse> getAnalysis(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ResponseEntity.ok(portfolioAnalysisService.analyze(userId));
    }
}