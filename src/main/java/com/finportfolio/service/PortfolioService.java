package com.finportfolio.service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finportfolio.dto.PortfolioItemRequest;
import com.finportfolio.dto.PortfolioItemResponse;
import com.finportfolio.dto.PortfolioSummaryResponse;
import com.finportfolio.entity.PortfolioItem;
import com.finportfolio.exception.ForbiddenException;
import com.finportfolio.exception.ResourceNotFoundException;
import com.finportfolio.repository.PortfolioItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioItemRepository portfolioItemRepository;
    private final MarketDataService marketDataService;

    @Transactional
    public PortfolioItemResponse addItem(Long userId, PortfolioItemRequest request) {
        PortfolioItem item = PortfolioItem.builder()
                .userId(userId)
                .assetType(request.assetType())
                .assetSymbol(request.assetSymbol().toUpperCase())
                .amount(request.amount())
                .buyPriceTry(request.buyPriceTry())
                .buyDate(request.buyDate() != null ? request.buyDate() : Instant.now())
                .build();
        item = portfolioItemRepository.save(item);

        BigDecimal currentPrice = marketDataService.getPriceTry(item.getAssetSymbol());
        return PortfolioItemResponse.from(item, currentPrice);
    }

    @Transactional(readOnly = true)
    public PortfolioSummaryResponse getPortfolio(Long userId) {
        List<PortfolioItem> items = portfolioItemRepository.findAllByUserId(userId);

        List<PortfolioItemResponse> responses = new ArrayList<>();
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalCurrent = BigDecimal.ZERO;

        for (PortfolioItem item : items) {
            BigDecimal currentPrice = marketDataService.getPriceTry(item.getAssetSymbol());
            PortfolioItemResponse resp = PortfolioItemResponse.from(item, currentPrice);
            responses.add(resp);

            totalInvested = totalInvested.add(resp.investedValueTry());
            totalCurrent = totalCurrent.add(resp.currentValueTry());
        }

        BigDecimal totalProfitLoss = totalCurrent.subtract(totalInvested);
        BigDecimal totalPercent = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            totalPercent = totalProfitLoss
                    .divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return new PortfolioSummaryResponse(
                responses,
                totalInvested,
                totalCurrent,
                totalProfitLoss,
                totalPercent
        );
    }

    @Transactional
    public PortfolioItemResponse updateItem(Long userId, Long itemId, PortfolioItemRequest request) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
        		.orElseThrow(() -> new ResourceNotFoundException("Yatirim bulunamadi"));

        if (!item.getUserId().equals(userId)) {
        	throw new ForbiddenException("Bu yatirim size ait degil");
        }

        item.setAssetType(request.assetType());
        item.setAssetSymbol(request.assetSymbol().toUpperCase());
        item.setAmount(request.amount());
        item.setBuyPriceTry(request.buyPriceTry());
        if (request.buyDate() != null) {
            item.setBuyDate(request.buyDate());
        }
        item = portfolioItemRepository.save(item);

        BigDecimal currentPrice = marketDataService.getPriceTry(item.getAssetSymbol());
        return PortfolioItemResponse.from(item, currentPrice);
    }

    @Transactional
    public void deleteItem(Long userId, Long itemId) {
    		PortfolioItem item = portfolioItemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Yatirim bulunamadi"));

    if (!item.getUserId().equals(userId)) {
        throw new ForbiddenException("Bu yatirim size ait degil");
    }
    portfolioItemRepository.delete(item);
}
}