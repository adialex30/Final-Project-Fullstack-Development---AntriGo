package com.antrigo.backend.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // ConcurrentHashMap sebelumnya gak pernah dibersihkan — tiap IP unik yang pernah checkout
    // nambah entry permanen selama JVM hidup (slow leak di uptime lama + traffic publik).
    // Caffeine (sudah jadi dependency project buat cache lain) otomatis evict IP yang gak aktif.
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(10_000)
            .build();
    private final int checkoutPerMinute;

    public RateLimitFilter(@Value("${app.rate-limit.checkout-per-minute:20}") int checkoutPerMinute) {
        this.checkoutPerMinute = checkoutPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        boolean isCheckout = "POST".equalsIgnoreCase(request.getMethod())
                && "/api/v1/orders".equals(request.getRequestURI());

        if (isCheckout) {
            String clientKey = clientKey(request);
            Bucket bucket = buckets.get(clientKey, k -> newBucket());
            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Terlalu banyak percobaan checkout, coba lagi sebentar\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(checkoutPerMinute,
                Refill.greedy(checkoutPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded : request.getRemoteAddr();
    }
}
