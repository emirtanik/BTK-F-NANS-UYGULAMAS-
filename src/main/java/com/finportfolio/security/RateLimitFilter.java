package com.finportfolio.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finportfolio.dto.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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
 * Login endpoint'ine dakikada 5 istek limiti uygular.
 * Brute force saldirilarini engeller.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_LOGIN_ATTEMPTS_PER_MINUTE = 5;
    private static final long WINDOW_MS = 60_000;

    private final ConcurrentHashMap<String, RateBucket> loginBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Sadece login icin rate limit uygula
        if ("POST".equals(method) && "/api/auth/login".equals(path)) {
            String ip = extractIp(request);

            if (!isAllowed(loginBuckets, ip, MAX_LOGIN_ATTEMPTS_PER_MINUTE)) {
                log.warn("Rate limit asildi - IP: {}", ip);
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

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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