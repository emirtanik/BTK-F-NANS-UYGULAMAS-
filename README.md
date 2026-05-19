# FinPortfolio

Türkçe finansal portföy takip ve AI asistan uygulaması. Altın, gümüş, döviz ve kripto yatırımlarını takip eder, gerçek zamanlı fiyat akışı sağlar, teknik analiz (RSI/SMA/Momentum/Volatilite) yapar ve Gemini AI ile kişiselleştirilmiş danışmanlık sunar.

## Teknolojiler

- **Backend:** Java 21, Spring Boot 3.3.5, Spring Security, JWT, H2 (file-based), Spring Data JPA, WebSocket
- **Frontend:** React 19, Vite, Axios, React Router 7
- **AI:** Google Gemini 2.0 Flash
- **Veri:** CoinGecko (kripto), ExchangeRate API (döviz), Gold-API (altın/gümüş)

## Hızlı Başlangıç

### Backend
```bash
mvnw spring-boot:run
```
Backend `http://localhost:8080` üzerinde çalışır.

### Frontend
```bash
cd frontend
npm install
npm run dev
```
Frontend `http://localhost:5173` üzerinde çalışır.

## Demo Hesap

Backend ilk açılışta otomatik bir demo hesap ve örnek yatırımlar yükler:

- **E-posta:** `demo@finportfolio.com`
- **Şifre:** `Demo1234`

Demo hesapta hazır olarak gelenler:
- 5 yatırım (Gram Altın, BTC, ETH, USD, EUR)
- 2 fiyat alarmı (BTC > 4.000.000 ₺, GRAM_ALTIN < 6.500 ₺)

## Özellikler

### Akıllı AI Asistan
Gemini AI'a her sohbette portföy özetin (alış fiyatları, güncel değerler, kar/zarar, çeşitlendirme), risk skorun ve mevcut piyasa fiyatları beslenir. Yanıtlar genel tavsiye değil, **senin durumuna özel** olur.

### Teknik Analiz
Kripto varlıklar için profesyonel göstergeler:
- **RSI 14** (aşırı alım/satım sinyalleri)
- **SMA 20/50** (hareketli ortalamalar)
- **Momentum** (7/30 günlük değişim)
- **Volatilite** (standart sapma)
- **Genel trend** (yükseliş/düşüş/yatay) + Türkçe yorum

Kullanıcı chatbot'a "BTC almalı mıyım?" diye sorduğunda, AI tüm bu göstergeleri kullanarak somut analiz yapar.

### Diğer
- JWT auth + Refresh token + Rate limiting (login için)
- Portföy CRUD + kar/zarar hesabı + çeşitlendirme skoru
- Fiyat alarmı (5 dakikada bir kontrol)
- Bildirim sistemi
- Audit log
- Geçmiş senaryo simülatörü (geçmiş tarihte alsaydım ne olurdu)
- Real-time WebSocket fiyat akışı (5sn)
- Karşılaştırmalı snapshot (son ziyaretten beri ne değişti)

## API Dökümanı

Swagger UI: `http://localhost:8080/swagger-ui.html`

### Önemli Endpoint'ler
- `POST /api/auth/login` — Giriş
- `GET /api/market/prices` — Tüm fiyatlar (public)
- `GET /api/market/analysis/BTC` — Teknik analiz (public)
- `GET /api/market/history/BTC?interval=1h&limit=24` — Tarihsel veri
- `GET /api/portfolio` — Kendi portföyün
- `GET /api/portfolio/analysis` — Risk + çeşitlendirme skoru
- `POST /api/chat/message` — AI asistana mesaj
- `GET /api/chat/welcome` — Login karşılama mesajı
- `POST /api/simulation` — Geçmiş senaryo simülatörü
- `WS /ws/prices` — Canlı fiyat akışı

## H2 Console

Gelişme sırasında veritabanını incelemek için:

`http://localhost:8080/h2-console`

- **JDBC URL:** `jdbc:h2:file:./data/finportfolio;AUTO_SERVER=TRUE`
- **User:** `sa`
- **Password:** (boş bırak)

## Notlar

- API key (`app.gemini.api-key`) yarışma demosu için `application.properties`'te yazılı durumda. Üretim ortamında environment variable olarak verilmesi önerilir.
- Demo data otomatik yüklenir (`app.demo.enabled=true`). Üretimde `false` yapılmalı.
- JWT token süresi 8 saat (geliştirme rahatlığı için). Üretimde 15 dakika önerilir.

## Mimari

```
src/main/java/com/finportfolio/
├── config/         # SecurityConfig, WebSocketConfig, DemoDataLoader
├── controller/     # REST endpoint'ler
├── dto/            # Request/Response sınıfları
├── entity/         # JPA entity'leri
├── repository/     # Spring Data JPA
├── security/       # JWT filter, rate limit
├── service/        # İş mantığı
│   └── external/   # CoinGecko, ExchangeRate, Gold API client'leri
└── websocket/      # PriceStreamHandler
```
