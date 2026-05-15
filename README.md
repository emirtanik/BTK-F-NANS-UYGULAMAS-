# FinPortfolio Backend

Türkiye'ye özel finansal portföy takip ve analiz sistemi. Altın, gümüş, döviz ve kripto varlıklarınızı tek yerden yönetir, yapay zeka destekli analizler sunar.

## Yarışma Bilgileri

**Tema:** Finans + E-ticaret
**Ekip:** 3 kişi
- Backend: Emir
- Frontend: Mert
- Siber Güvenlik: Ebubekir

## Öne Çıkan Özellikler

- 14 farklı varlık desteği (BTC, ETH, BNB, SOL, XRP, ADA, Gram/Çeyrek/Yarım/Tam Altın, Gümüş, USD, EUR, GBP)
- Canlı fiyat akışı (Binance + ExchangeRate + Gold-API)
- WebSocket ile gerçek zamanlı dashboard güncelleme
- Gemini destekli AI portföy asistanı (Türkçe konuşan, fallback'li)
- "Son girişten beri kar/zarar" snapshot sistemi
- "Ne olurdu eğer?" tarihsel simülatör
- Risk skoru ve çeşitlendirme analizi (HHI tabanlı)
- Fiyat alarmı ve bildirim sistemi (5dk scheduled job)
- JWT auth + IP bazlı rate limit + audit log

## Teknik Stack

- **Java 21** + **Spring Boot 3.3**
- **H2 Database** (file-based, dev) — production için PostgreSQL'e geçiş hazır
- **Spring Security** + JWT (jjwt 0.12.6)
- **Spring WebSocket** — push-based price stream
- **Google Gemini 2.0 Flash** — AI asistan
- **Binance API** — kripto fiyatları
- **ExchangeRate API** — döviz kurları
- **Gold-API** — altın/gümüş ons fiyatı
- **SpringDoc OpenAPI 3** — Swagger UI

## Çalıştırma

```bash
# 1. Repo'yu klonla
git clone <repo-url>
cd finportfolio-backend

# 2. application.properties içine Gemini API key gir
# app.gemini.api-key=AIzaSy...

# 3. Çalıştır
./mvnw spring-boot:run

# Veya Eclipse/IntelliJ'de:
# FinportfolioBackendApplication.java → Run As → Spring Boot App
```

Tarayıcıda aç:
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:file:./data/finportfolio`, user: `sa`, pass: boş)

## Demo Hesap

Uygulama her başladığında otomatik demo kullanıcı yükler:


Email: demo@finportfolio.com
Şifre: Demo1234

Portföyde 5 yatırım, 2 fiyat alarmı hazır gelir.

## API Endpoint Özeti

### 1. Kimlik Doğrulama
- `POST /api/auth/register` — Yeni kullanıcı kaydı
- `POST /api/auth/login` — Giriş, JWT alır

### 2. Piyasa Verileri
- `GET /api/market/prices` — Tüm anlık fiyatlar
- `GET /api/market/prices/{symbol}` — Tek sembol
- `GET /api/market/history/{symbol}?interval=1h&limit=100` — OHLCV mum verisi
- `ws://localhost:8080/ws/prices` — Canlı fiyat akışı

### 3. Portföy
- `GET /api/portfolio` — Portföy listesi + güncel değer + kar/zarar
- `POST /api/portfolio/items` — Yatırım ekle
- `DELETE /api/portfolio/items/{id}` — Yatırım sil
- `GET /api/portfolio/analysis` — Risk skoru ve öneriler

### 4. AI Asistan
- `GET /api/chat/welcome` — Login sonrası karşılama özeti
- `POST /api/chat/message` — Serbest sohbet

### 5. Simülatör
- `POST /api/simulation` — "Ne olurdu eğer?" hesabı

### 6. Bildirimler
- `POST /api/alerts` — Fiyat alarmı kur
- `GET /api/alerts` — Alarmlarımı listele
- `GET /api/notifications` — Bildirimlerim
- `GET /api/notifications/count` — Okunmamış sayısı

### 7. Audit
- `GET /api/audit/my-logs` — Kendi log'larım

## Güvenlik Önlemleri

- ✅ JWT access token (15 dk) + refresh token (7 gün)
- ✅ BCrypt password hashing (strength 12)
- ✅ IP bazlı rate limiting (login 5/dakika)
- ✅ Audit log — tüm önemli aksiyonlar
- ✅ CORS yapılandırması
- ✅ Validation (jakarta validation)
- ✅ Tutarlı error response (ApiError)

## Mimari Notlar

- **In-memory cache** (30 sn TTL) — dış API'lere yük bindirmemek için
- **Async audit logging** — ana akışı yavaşlatmaz
- **Graceful fallback** — Gemini API erişilemediğinde local Türkçe yanıt sistemi devreye girer
- **Retry mantığı** — Gemini için 3 deneme + 2 sn bekleme
- **Snapshot karşılaştırma** — kullanıcı her girişte "son ziyaretten beri ne değişti" görür

## Geliştirme Yol Haritası

- [ ] 2FA TOTP (Google Authenticator)
- [ ] Email doğrulama
- [ ] Frontend (Mert tarafından)
- [ ] Penetrasyon testleri (Ebubekir tarafından)
- [ ] PostgreSQL migration (production için)
- [ ] Docker containerization
