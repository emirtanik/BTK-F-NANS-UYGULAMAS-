package com.finportfolio.config;

import com.finportfolio.entity.*;
import com.finportfolio.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Uygulama her basladiginda demo kullanici ve portfoy yukler.
 * application.properties'te app.demo.enabled=false yapilirsa devre disi kalir.
 *
 * Lokal gelistirmede app.demo.password (varsayilan: Demo1234) kullanilir.
 * Production'da app.demo.enabled=false yapilmalidir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DemoDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final PriceAlertRepository priceAlertRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo.enabled:false}")
    private boolean demoEnabled;

    @Value("${app.demo.password:Demo1234}")
    private String demoPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (!demoEnabled) {
            log.info("Demo veri devre disi");
            return;
        }

        var existingDemo = userRepository.findByEmail("demo@finportfolio.com");
        if (existingDemo.isPresent()) {
            User demoUser = existingDemo.get();
            demoUser.setPasswordHash(passwordEncoder.encode(demoPassword));
            userRepository.save(demoUser);
            log.info("Demo kullanici mevcut, sifre guncellendi: demo@finportfolio.com");
            return;
        }

        log.info("Demo veri yukleniyor...");

        User demoUser = User.builder()
                .email("demo@finportfolio.com")
                .passwordHash(passwordEncoder.encode(demoPassword))
                .fullName("Demo Kullanici")
                .isActive(true)
                .build();
        demoUser = userRepository.save(demoUser);

        Long userId = demoUser.getId();
        log.info("Demo kullanici olusturuldu: ID={}, email=demo@finportfolio.com", userId);

        // Demo portfoy
        portfolioItemRepository.save(PortfolioItem.builder()
                .userId(userId).assetType(AssetType.GOLD).assetSymbol("GRAM_ALTIN")
                .amount(new BigDecimal("100")).buyPriceTry(new BigDecimal("6200"))
                .buyDate(Instant.now().minus(60, ChronoUnit.DAYS)).build());

        portfolioItemRepository.save(PortfolioItem.builder()
                .userId(userId).assetType(AssetType.CRYPTO).assetSymbol("BTC")
                .amount(new BigDecimal("0.025")).buyPriceTry(new BigDecimal("3450000"))
                .buyDate(Instant.now().minus(45, ChronoUnit.DAYS)).build());

        portfolioItemRepository.save(PortfolioItem.builder()
                .userId(userId).assetType(AssetType.CRYPTO).assetSymbol("ETH")
                .amount(new BigDecimal("1.5")).buyPriceTry(new BigDecimal("115000"))
                .buyDate(Instant.now().minus(30, ChronoUnit.DAYS)).build());

        portfolioItemRepository.save(PortfolioItem.builder()
                .userId(userId).assetType(AssetType.CURRENCY).assetSymbol("USD")
                .amount(new BigDecimal("500")).buyPriceTry(new BigDecimal("42.50"))
                .buyDate(Instant.now().minus(20, ChronoUnit.DAYS)).build());

        portfolioItemRepository.save(PortfolioItem.builder()
                .userId(userId).assetType(AssetType.CURRENCY).assetSymbol("EUR")
                .amount(new BigDecimal("300")).buyPriceTry(new BigDecimal("48.20"))
                .buyDate(Instant.now().minus(15, ChronoUnit.DAYS)).build());

        priceAlertRepository.save(PriceAlert.builder()
                .userId(userId).assetSymbol("BTC").conditionType(AlertCondition.PRICE_ABOVE)
                .thresholdValue(new BigDecimal("4000000")).isActive(true).isTriggered(false).build());

        priceAlertRepository.save(PriceAlert.builder()
                .userId(userId).assetSymbol("GRAM_ALTIN").conditionType(AlertCondition.PRICE_BELOW)
                .thresholdValue(new BigDecimal("6500")).isActive(true).isTriggered(false).build());

        log.info("Demo veri yuklendi: 5 yatirim, 2 alarm");
    }
}