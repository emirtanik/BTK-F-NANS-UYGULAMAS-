package com.finportfolio.service;

import com.finportfolio.dto.SimulationRequest;
import com.finportfolio.dto.SimulationResponse;
import com.finportfolio.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService {

    private final HistoryService historyService;
    private final MarketDataService marketDataService;

    public SimulationResponse simulate(SimulationRequest request) {
        String symbol = request.assetSymbol().toUpperCase();
        LocalDate date = request.investmentDate();
        BigDecimal investment = request.investmentTry();

        // 1. Tarihsel fiyati bul
        BigDecimal historicalPrice = historyService.getHistoricalPrice(symbol, date);
        if (historicalPrice == null || historicalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    "Bu tarih veya sembol icin tarihsel veri bulunamadi: " + symbol + " / " + date,
                    HttpStatus.NOT_FOUND
            );
        }

        // 2. Guncel fiyati al
        BigDecimal currentPrice = marketDataService.getPriceTry(symbol);
        if (currentPrice == null) {
            throw new BusinessException(
                    "Guncel fiyat bulunamadi: " + symbol,
                    HttpStatus.NOT_FOUND
            );
        }

        // 3. Hesaplamalar
        BigDecimal amountBought = investment.divide(historicalPrice, 8, RoundingMode.HALF_UP);
        BigDecimal currentValue = amountBought.multiply(currentPrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal profitLoss = currentValue.subtract(investment);

        BigDecimal profitLossPercent = profitLoss
                .divide(investment, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        long daysHeld = ChronoUnit.DAYS.between(date, LocalDate.now());

        // 4. Hikaye anlatan ozet
        String summary = buildSummary(symbol, date, investment, historicalPrice,
                currentPrice, amountBought, currentValue, profitLoss, profitLossPercent, daysHeld);

        return new SimulationResponse(
                symbol,
                date,
                investment,
                historicalPrice.setScale(4, RoundingMode.HALF_UP),
                currentPrice.setScale(4, RoundingMode.HALF_UP),
                amountBought,
                currentValue,
                profitLoss.setScale(2, RoundingMode.HALF_UP),
                profitLossPercent.setScale(2, RoundingMode.HALF_UP),
                daysHeld,
                summary
        );
    }

    private String buildSummary(String symbol, LocalDate date, BigDecimal investment,
                                 BigDecimal historicalPrice, BigDecimal currentPrice,
                                 BigDecimal amountBought, BigDecimal currentValue,
                                 BigDecimal profitLoss, BigDecimal percent, long days) {

        String result = profitLoss.compareTo(BigDecimal.ZERO) >= 0 ? "kazanırdın" : "kaybederdin";
        BigDecimal absLoss = profitLoss.abs();

        return String.format(
                "%s tarihinde %s ₺ ile %s alsaydın, o günkü fiyat %s ₺ idi. " +
                "Bugün, %d gün sonra, elindeki %s adet %s'nin değeri %s ₺ olurdu. " +
                "Yani %s ₺ %s (%%%s).",
                date,
                investment.toPlainString(),
                symbol,
                historicalPrice.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                days,
                amountBought.setScale(6, RoundingMode.HALF_UP).toPlainString(),
                symbol,
                currentValue.toPlainString(),
                absLoss.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                result,
                percent.setScale(2, RoundingMode.HALF_UP).toPlainString()
        );
    }
}