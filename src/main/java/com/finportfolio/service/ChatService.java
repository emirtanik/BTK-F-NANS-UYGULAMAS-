package com.finportfolio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.finportfolio.dto.ChatResponse;
import com.finportfolio.dto.PortfolioAnalysisResponse;
import com.finportfolio.dto.WelcomeChatResponse;
import com.finportfolio.entity.PortfolioItem;
import com.finportfolio.repository.PortfolioItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final GeminiService geminiService;
    private final MarketDataService marketDataService;
    private final PortfolioItemRepository portfolioItemRepository;
    private final SnapshotService snapshotService;
    private final PortfolioAnalysisService portfolioAnalysisService;

    private static final String CHAT_SYSTEM_PROMPT = """
            Sen FinPortfolio adlı Türkçe konuşan, samimi ve bilgili bir finansal portföy asistanısın.
            Kullanıcılar altın, gümüş, döviz ve kripto yatırımları yapıyor.
            Yanıtların kısa (3-5 cümle), net ve Türkçe olmalı. Profesyonel ama soğuk olma, samimi bir arkadaş gibi konuş.
            Asla kesin yatırım tavsiyesi verme - "şunu al, şunu sat" deme. Bunun yerine genel bilgi ver, riskleri anlat, kullanıcının kendi kararını vermesi gerektiğini söyle.
            Türk Lirası fiyatlarıyla konuş. Sayıları 1.234,56 ₺ formatında yaz.
            Emoji kullanma, profesyonel kal.
            """;

    private static final String ADVISOR_SYSTEM_PROMPT = """
            Sen FinPortfolio'un deneyimli portföy danışmanısın. Görevin kullanıcıya akıl vermek, riskleri ve seçenekleri açıklamaktır.
            Yanıtların Türkçe, yapılandırılmış ve net olsun; gerektiğinde madde işaretleri kullan.
            Kesin al/sat emri verme; bunun yerine senaryolar, riskler ve çeşitlendirme önerileri sun.
            Portföy analiz verilerine dayan; kullanıcının mevcut dağılımını dikkate al.
            Türk Lirası kullan. Emoji kullanma.
            """;

    /**
     * Gemini'nin verdigi yanit "ulasamiyorum" iceriyor mu kontrol eder.
     */
    private boolean isFallbackResponse(String reply) {
        if (reply == null) return true;
        String lower = reply.toLowerCase();
        return lower.contains("ulaşamıyorum") || lower.contains("ulasamiyorum")
                || lower.contains("hata detayi") || lower.contains("hata detayı");
    }

    public WelcomeChatResponse welcome(Long userId) {
        List<PortfolioItem> items = portfolioItemRepository.findAllByUserId(userId);

        if (items.isEmpty()) {
            String geminiReply = geminiService.generate(
                    CHAT_SYSTEM_PROMPT,
                    "Kullanıcı yeni giriş yaptı ama henüz portföyü boş. Onu sıcak bir şekilde karşıla, " +
                    "altın, gümüş, döviz veya kripto yatırımlarını eklemesi için yönlendir. 2-3 cümle."
            );

            String message = isFallbackResponse(geminiReply)
                    ? "Merhaba! FinPortfolio'ya hoş geldin. Henüz portföyünde bir varlık bulunmuyor. " +
                      "Altın, gümüş, döviz veya kripto yatırımlarını ekleyerek piyasayı buradan takip edebilirsin."
                    : geminiReply;

            return new WelcomeChatResponse(message, List.of(), BigDecimal.ZERO, BigDecimal.ZERO);
        }

        Map<String, BigDecimal> lastSnapshotPrices = snapshotService.getLastSnapshotPrices(userId);
        Instant lastSnapshotTime = snapshotService.getLastSnapshotTime(userId);
        boolean isFirstLogin = lastSnapshotPrices.isEmpty();

        List<WelcomeChatResponse.AssetChange> changes = new ArrayList<>();
        BigDecimal totalProfitLoss = BigDecimal.ZERO;
        BigDecimal totalInvested = BigDecimal.ZERO;
        StringBuilder summaryForGemini = new StringBuilder();

        String timeAgo = "";
        if (lastSnapshotTime != null) {
            long hours = ChronoUnit.HOURS.between(lastSnapshotTime, Instant.now());
            if (hours < 1) {
                long minutes = ChronoUnit.MINUTES.between(lastSnapshotTime, Instant.now());
                timeAgo = minutes + " dakika önce";
            } else if (hours < 24) {
                timeAgo = hours + " saat önce";
            } else {
                long days = hours / 24;
                timeAgo = days + " gün önce";
            }
        }

        if (isFirstLogin) {
            summaryForGemini.append("Kullanıcı bu uygulamaya ilk defa giriş yapıyor. Portföyündeki varlıklar:\n");
        } else {
            summaryForGemini.append("Kullanıcının son girişi ").append(timeAgo)
                    .append(". O zamandan beri portföyündeki değişimler:\n");
        }

        for (PortfolioItem item : items) {
            String symbol = item.getAssetSymbol();
            BigDecimal currentPrice = marketDataService.getPriceTry(symbol);
            if (currentPrice == null) continue;

            BigDecimal referencePrice;
            if (isFirstLogin) {
                referencePrice = item.getBuyPriceTry();
            } else {
                referencePrice = lastSnapshotPrices.getOrDefault(symbol, item.getBuyPriceTry());
            }

            BigDecimal amount = item.getAmount();
            BigDecimal referenceValue = referencePrice.multiply(amount);
            BigDecimal currentValue = currentPrice.multiply(amount);
            BigDecimal profitLoss = currentValue.subtract(referenceValue);

            BigDecimal changePercent = BigDecimal.ZERO;
            if (referencePrice.compareTo(BigDecimal.ZERO) > 0) {
                changePercent = currentPrice.subtract(referencePrice)
                        .divide(referencePrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            changes.add(new WelcomeChatResponse.AssetChange(
                    symbol, referencePrice, currentPrice, changePercent, profitLoss, amount
            ));

            totalProfitLoss = totalProfitLoss.add(profitLoss);
            totalInvested = totalInvested.add(referenceValue);

            summaryForGemini.append(String.format("- %s: %s adet, eski fiyat %s ₺, yeni fiyat %s ₺, fark %%%.2f, kar/zarar %.2f ₺%n",
                    symbol, amount, referencePrice, currentPrice, changePercent, profitLoss));
        }

        BigDecimal totalPercent = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            totalPercent = totalProfitLoss.divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        summaryForGemini.append(String.format("%nToplam: %.2f ₺ (%%%.2f)%n", totalProfitLoss, totalPercent));

        if (isFirstLogin) {
            summaryForGemini.append("\nKullanıcıyı 2-3 cümleyle sıcak bir şekilde karşıla, portföyü hakkında genel bir yorum yap.");
        } else {
            summaryForGemini.append("\nKullanıcıyı 2-3 cümleyle karşıla, son ziyaretinden bu yana neler değiştiğini özetle. " +
                    "Pozitifse motive et, negatifse moralini bozmadan durumun normal olduğunu anlat. Süreyi (")
                    .append(timeAgo).append(") mutlaka mesajında belirt.");
        }

        String geminiReply = geminiService.generate(CHAT_SYSTEM_PROMPT, summaryForGemini.toString());

        // Gemini yanit veremedi mi? Kendi akilli ozet uretelim.
        String botMessage = isFallbackResponse(geminiReply)
                ? buildLocalWelcomeMessage(items, totalProfitLoss, totalPercent, timeAgo, isFirstLogin)
                : geminiReply;

        snapshotService.captureSnapshotForUser(userId);

        return new WelcomeChatResponse(botMessage, changes, totalProfitLoss, totalPercent);
    }

    public ChatResponse chat(Long userId, String userMessage) {
        String context = buildPortfolioContext(userId);
        String fullMessage = context + "\n\nKullanıcı sorusu: " + userMessage;
        String geminiReply = geminiService.generate(CHAT_SYSTEM_PROMPT, fullMessage);

        String reply = isFallbackResponse(geminiReply)
                ? buildLocalChatReply(userId, userMessage)
                : geminiReply;

        return ChatResponse.of(reply);
    }

    /**
     * Danışman: girişte portföy analizine dayalı özet tavsiye.
     */
    public ChatResponse adviceWelcome(Long userId) {
        PortfolioAnalysisResponse analysis = portfolioAnalysisService.analyze(userId);
        String prompt = buildAnalysisPrompt(analysis) +
                "\n\nKullanıcıya danışman olarak kısa bir karşılama ve 2-4 maddelik kişisel öneri özeti yaz.";

        String geminiReply = geminiService.generate(ADVISOR_SYSTEM_PROMPT, prompt);
        String reply = isFallbackResponse(geminiReply)
                ? buildLocalAdviceWelcome(analysis)
                : geminiReply;

        return ChatResponse.of(reply);
    }

    /**
     * Danışman: serbest soruya portföy + analiz bağlamında akıl verir.
     */
    public ChatResponse advise(Long userId, String userMessage) {
        PortfolioAnalysisResponse analysis = portfolioAnalysisService.analyze(userId);
        String context = buildPortfolioContext(userId) + "\n\n" + buildAnalysisPrompt(analysis);
        String fullMessage = context + "\n\nKullanıcı sorusu: " + userMessage;

        String geminiReply = geminiService.generate(ADVISOR_SYSTEM_PROMPT, fullMessage);
        String reply = isFallbackResponse(geminiReply)
                ? buildLocalAdviceReply(analysis, userMessage)
                : geminiReply;

        return ChatResponse.of(reply);
    }

    private String buildAnalysisPrompt(PortfolioAnalysisResponse analysis) {
        StringBuilder sb = new StringBuilder("Portföy analizi:\n");
        sb.append("- Risk skoru: ").append(analysis.riskScore()).append(" (").append(analysis.riskLevel()).append(")\n");
        sb.append("- Çeşitlendirme skoru: ").append(analysis.diversificationScore()).append("/100\n");
        sb.append("- Toplam değer: ").append(analysis.totalValueTry()).append(" ₺\n");
        sb.append("- Varlık sayısı: ").append(analysis.totalAssets()).append("\n");

        if (!analysis.recommendations().isEmpty()) {
            sb.append("Öneriler:\n");
            analysis.recommendations().forEach(r -> sb.append("  • ").append(r).append("\n"));
        }
        if (!analysis.warnings().isEmpty()) {
            sb.append("Uyarılar:\n");
            analysis.warnings().forEach(w -> sb.append("  • ").append(w).append("\n"));
        }
        return sb.toString();
    }

    private String buildLocalAdviceWelcome(PortfolioAnalysisResponse analysis) {
        StringBuilder sb = new StringBuilder("Merhaba, ben portföy danışmanın. ");
        sb.append(String.format("Risk skorun %d (%s), çeşitlendirme skorun %s/100. ",
                analysis.riskScore(), analysis.riskLevel(), analysis.diversificationScore()));

        if (!analysis.warnings().isEmpty()) {
            sb.append("\n\nDikkat etmen gerekenler:\n");
            analysis.warnings().forEach(w -> sb.append("• ").append(w).append("\n"));
        }
        if (!analysis.recommendations().isEmpty()) {
            sb.append("\nÖnerilerim:\n");
            analysis.recommendations().forEach(r -> sb.append("• ").append(r).append("\n"));
        }
        return sb.toString().trim();
    }

    private String buildLocalAdviceReply(PortfolioAnalysisResponse analysis, String userMessage) {
        String lower = userMessage.toLowerCase();

        if (lower.contains("risk")) {
            return String.format(
                    "Risk skorun %d ve seviye %s. Kripto ağırlığın yüksekse volatilite artar; altın ve döviz ekleyerek dengeleyebilirsin.",
                    analysis.riskScore(), analysis.riskLevel());
        }

        if (!analysis.recommendations().isEmpty()) {
            return "Portföyüne göre öncelikli önerilerim:\n• " +
                    String.join("\n• ", analysis.recommendations());
        }

        return "Sorunu portföy verilerinle birlikte değerlendirdim. Daha net öneri için risk, çeşitlendirme veya belirli bir varlık hakkında soru sorabilirsin.";
    }

    private String buildPortfolioContext(Long userId) {
        List<PortfolioItem> items = portfolioItemRepository.findAllByUserId(userId);
        if (items.isEmpty()) {
            return "Kullanıcının henüz portföyü yok.";
        }

        StringBuilder sb = new StringBuilder("Kullanıcının portföyü:\n");
        for (PortfolioItem item : items) {
            BigDecimal currentPrice = marketDataService.getPriceTry(item.getAssetSymbol());
            sb.append(String.format("- %s: %s adet, alış %s ₺, güncel %s ₺\n",
                    item.getAssetSymbol(), item.getAmount(), item.getBuyPriceTry(),
                    currentPrice != null ? currentPrice : "bilinmiyor"));
        }
        return sb.toString();
    }

    /**
     * Gemini calismadiginda welcome mesaji uretir.
     * Portfoy verisinden gercek Turkce ozet cikarir.
     */
    private String buildLocalWelcomeMessage(List<PortfolioItem> items, BigDecimal totalPnl,
                                             BigDecimal totalPercent, String timeAgo, boolean isFirstLogin) {
        StringBuilder sb = new StringBuilder();

        if (isFirstLogin) {
            sb.append("Merhaba, FinPortfolio'ya hoş geldin. ");
        } else if (!timeAgo.isEmpty()) {
            sb.append("Tekrar hoş geldin. Son ziyaretin ").append(timeAgo).append(". ");
        } else {
            sb.append("Tekrar hoş geldin. ");
        }

        sb.append("Portföyünde toplam ").append(items.size()).append(" farklı varlık bulunuyor. ");

        if (totalPnl.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Genel olarak %.2f ₺ kârdasın (%%%.2f). ",
                    totalPnl.doubleValue(), totalPercent.doubleValue()));
            sb.append("Dengeli bir portföy oluşturmuşsun, çeşitlendirmeye devam etmek riskini azaltır.");
        } else if (totalPnl.compareTo(BigDecimal.ZERO) < 0) {
            sb.append(String.format("Şu an %.2f ₺ zarardasın (%%%.2f). ",
                    totalPnl.abs().doubleValue(), totalPercent.abs().doubleValue()));
            sb.append("Piyasa dalgalı olabilir, kısa vadeli iniş çıkışlara takılma. Uzun vadeli stratejine sadık kal.");
        } else {
            sb.append("Portföyün şu an alış değerinde, ne kâr ne zarar var. Piyasayı takip etmeye devam et.");
        }

        return sb.toString();
    }

    /**
     * Gemini calismadiginda chat mesaji uretir.
     * Anahtar kelimelere gore akilli Turkce yanit verir.
     */
    private String buildLocalChatReply(Long userId, String userMessage) {
        String lower = userMessage.toLowerCase();
        List<PortfolioItem> items = portfolioItemRepository.findAllByUserId(userId);

        if (lower.contains("dengeli") || lower.contains("çeşit") || lower.contains("denge")) {
            if (items.size() <= 1) {
                return "Portföyünde sadece " + items.size() + " varlık var. Daha dengeli olması için farklı varlık sınıflarına " +
                        "(altın, döviz, kripto) dağılmayı düşünebilirsin. Tek bir varlığa bağlı kalmak risklidir.";
            }
            return "Portföyünde " + items.size() + " farklı varlık var, bu çeşitlendirme açısından iyi. " +
                    "İdeal dağılım kişisel risk toleransına bağlı, ama genelde %30-40 kripto, %30 altın/gümüş, %30 döviz dengesi " +
                    "uzun vadede istikrarlı kabul edilir.";
        }

        if (lower.contains("altın")) {
            return "Altın, enflasyona karşı geleneksel bir koruma aracıdır. Fiyatları küresel dolar kurundan ve faiz oranlarından etkilenir. " +
                    "Portföyünün %20-30'unu altında tutmak çeşitlendirme açısından mantıklıdır, ama tamamını tek varlığa bağlamak risklidir.";
        }

        if (lower.contains("dolar") || lower.contains("usd") || lower.contains("döviz") || lower.contains("euro")) {
            return "Döviz pozisyonları, TL'ye karşı kur riskini dengelemek için kullanılabilir. Ancak kısa vadeli kur hareketlerini tahmin etmek zordur. " +
                    "Uzun vadeli ve dengeli bir döviz pozisyonu daha sağlıklıdır.";
        }

        if (lower.contains("kripto") || lower.contains("bitcoin") || lower.contains("btc") || lower.contains("ethereum") || lower.contains("eth")) {
            return "Kripto varlıklar yüksek volatiliteye sahiptir, hem büyük kazanç hem büyük kayıp potansiyeli var. " +
                    "Portföyünün küçük bir kısmını (genellikle %10-20) ayırman ve sadece kaybetmeyi göze alabileceğin miktarı yatırman önerilir.";
        }

        if (lower.contains("zarar") || lower.contains("kayb") || lower.contains("düş")) {
            return "Piyasalar dalgalıdır, kısa vadeli düşüşler her zaman olur. Önemli olan uzun vadeli stratejine sadık kalmak. " +
                    "Panikle satmak yerine durumu objektif değerlendirmek, gerekirse pozisyonunu çeşitlendirmek daha mantıklı olur.";
        }

        if (lower.contains("ne yapmalı") || lower.contains("öneri") || lower.contains("tavsiye")) {
            return "Sana kesin tavsiye veremem çünkü her yatırımcının durumu farklı. Genel kural: çeşitlendirme yap, kaybetmeyi göze alabileceğin parayı yatır, " +
                    "kısa vadeli dalgalanmalara takılma. Daha spesifik bir soru sorarsan yardımcı olmaya çalışırım.";
        }

        if (items.isEmpty()) {
            return "Henüz portföyünde varlık yok, önce yatırımlarını ekleyerek başlamanı öneririm. Sonra senin portföyüne özel sorular sorabilirsin.";
        }

        return "Sorduğun konuda sana spesifik bilgi verebilmem için biraz daha detay almam gerek. " +
                "Portföyün hakkında, belirli bir varlık (altın, dolar, kripto) hakkında veya genel piyasa durumu hakkında soru sorabilirsin.";
    }
}