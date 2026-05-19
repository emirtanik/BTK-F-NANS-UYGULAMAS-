package com.finportfolio.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finportfolio.dto.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IP bazli rate limit filter.
 *
 * - /api/auth/login: dakikada 5 istek
 * - /api/auth/register: dakikada 3 istek
 * Brute force ve hesap olusturma kotuye kullanimini engeller.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_LOGIN_PER_MINUTE = 5;
    private static final int MAX_REGISTER_PER_MINUTE = 3;
    private static final long WINDOW_MS = 60_000;

    private final ConcurrentHashMap<String, RateBucket> loginBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RateBucket> registerBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.security.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equals(method) && "/api/auth/login".equals(path)) {
            if (!isAllowed(loginBuckets, extractIp(request), MAX_LOGIN_PER_MINUTE)) {
                log.warn("Login rate limit asildi - IP: {}", extractIp(request));
                writeRateLimitResponse(response, request);
                return;
            }
        } else if ("POST".equals(method) && "/api/auth/register".equals(path)) {
            if (!isAllowed(registerBuckets, extractIp(request), MAX_REGISTER_PER_MINUTE)) {
                log.warn("Register rate limit asildi - IP: {}", extractIp(request));
                writeRateLimitResponse(response, request);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(ConcurrentHashMap<String, RateBucket> buckets, String key, int maxRequests) {
        long now = Instant.now().toEpochMilli();
        RateBucket bucket = buckets.compute(key, (k, b) -> {
            if (b == null || now - b.windowStart > WINDOW_MS) {
                return new RateBucket(now);
            }
            return b;
        });

        int currentCount = bucket.count.incrementAndGet();
        return currentCount <= maxRequests;
    }

    /**
     * X-Forwarded-For header'i sadece guvenilir bir reverse-proxy arkasinda
     * calistigimizdan eminsek (app.security.trust-forwarded-headers=true) okuruz.
     * Aksi halde saldirgan bu header'i sahte yazip rate limit'i bypass edebilir.
     */
    private String extractIp(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
                String ip = request.getRemoteAddr();
        // IPv6 loopback'i IPv4'e normalize et (Windows localhost sorunu)
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

    private void writeRateLimitResponse(HttpServletResponse response, HttpServletRequest request) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError error = ApiError.of(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                "Cok fazla deneme yapildi. Lutfen 1 dakika bekleyin.",
                request.getRequestURI()
        );

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private static class RateBucket {
        final long windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        RateBucket(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}