package com.finportfolio.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.finportfolio.dto.PriceResponse;
import com.finportfolio.service.HistoryService;
import com.finportfolio.service.MarketDataService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "2. Piyasa Verileri", description = "Anlık fiyatlar ve tarihsel mum verisi")
public class MarketController {

    private final MarketDataService marketDataService;
    private final HistoryService historyService;
    @GetMapping("/prices")
    public ResponseEntity<List<PriceResponse>> allPrices() {
        return ResponseEntity.ok(marketDataService.getAllPrices());
    }

    @GetMapping("/prices/{symbol}")
    public ResponseEntity<BigDecimal> price(@PathVariable String symbol) {
        return ResponseEntity.ok(marketDataService.getPriceTry(symbol));
    }
    /**
     * Tarihsel OHLCV mum verisi (grafikler icin).
     *
     * Ornek: /api/market/history/BTC?interval=1h&limit=100
     * Interval: 1m, 5m, 15m, 1h, 4h, 1d, 1w
     * Limit: max 1000
     */
    @GetMapping("/history/{symbol}")
    public ResponseEntity<List<com.finportfolio.dto.CandleResponse>> history(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1h") String interval,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(historyService.getHistory(symbol, interval, limit));
    }
}