package com.clinic.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int CAPACITY = 100;
    private static final int REFILL_PER_SECOND = 10;
    
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String ip = getClientIp(request);
        TokenBucket bucket = buckets.computeIfAbsent(ip, k -> new TokenBucket(CAPACITY, REFILL_PER_SECOND));

        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded for IP: {}", ip);
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    // Visible for testing to speed up tests or reset limits
    public void resetLimit(String ip) {
        buckets.remove(ip);
    }

    private static class TokenBucket {
        private final long capacity;
        private final long refillRate; // tokens per second
        private double tokens;
        private long lastRefillTimestamp;

        public TokenBucket(long capacity, long refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.nanoTime();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            long duration = now - lastRefillTimestamp;
            double seconds = (double) duration / TimeUnit.SECONDS.toNanos(1);
            if (seconds > 0) {
                tokens = Math.min(capacity, tokens + (seconds * refillRate));
                lastRefillTimestamp = now;
            }
        }
    }
}
