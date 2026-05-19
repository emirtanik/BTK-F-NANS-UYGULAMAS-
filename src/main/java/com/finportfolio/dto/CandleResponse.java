package com.finportfolio.dto;

import java.math.BigDecimal;

/**
 * TradingView Lightweight Charts ile uyumlu OHLCV mum verisi.
 * time: Unix timestamp (saniye)
 */
public record CandleResponse(
        long time,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume
) {}