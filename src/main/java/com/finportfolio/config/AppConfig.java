package com.finportfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    // Default timeout ve user agent
                    request.getHeaders().set("User-Agent", "FinPortfolio/1.0");
                    return execution.execute(request, body);
                })
                .build();
    }
}