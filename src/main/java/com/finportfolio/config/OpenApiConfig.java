package com.finportfolio.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinPortfolio API")
                        .version("1.0.0")
                        .description("""
                                **Türkiye geneli finansal portföy takip ve analiz sistemi.**

                                Bu API altın, gümüş, döviz ve kripto varlıklarınızı tek yerden takip etmenizi sağlar.
                                Yapay zeka destekli portföy asistanı (Gemini), gerçek zamanlı fiyat akışı (WebSocket),
                                'son girişten beri kar/zarar' analizi, akıllı bildirim sistemi ve risk değerlendirme
                                gibi gelişmiş özellikler içerir.

                                ## Kullanım Akışı

                                1. `POST /api/auth/register` ile hesap aç
                                2. `POST /api/auth/login` ile giriş yap ve **accessToken** al
                                3. Sağ üstteki **Authorize** butonuna basıp token'ı yapıştır
                                4. Diğer endpoint'leri test et

                                ## Veri Kaynakları

                                - **Kripto:** Binance API (canlı)
                                - **Döviz:** ExchangeRate API (canlı)
                                - **Altın/Gümüş:** Gold-API ons fiyatı × USD/TRY hesabı
                                - **Asistan:** Google Gemini 2.0 Flash

                                ## Güvenlik

                                - JWT tabanlı kimlik doğrulama (15 dakika access token)
                                - Refresh token rotation
                                - BCrypt password hashing (strength 12)
                                - IP bazlı rate limiting (login için 5/dakika)
                                - Audit log (tüm aksiyonlar veritabanında)
                                """)
                        .contact(new Contact()
                                .name("FinPortfolio Ekibi")
                                .email("info@finportfolio.local"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Yerel geliştirme"),
                        new Server().url("https://api.finportfolio.local").description("Production (placeholder)")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token. `POST /api/auth/login` endpoint'inden alın.")));
    }
}