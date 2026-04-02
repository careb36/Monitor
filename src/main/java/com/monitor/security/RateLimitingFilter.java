package com.monitor.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servlet filter implementing per-IP rate limiting using Bucket4j.
 *
 * <p>Prevents brute-force attacks against HTTP Basic auth endpoints (CWE-307).
 * Each unique client IP gets its own token bucket. Buckets are created lazily
 * and cached in memory.
 *
 * <p>Configuration via application.yml:
 * <pre>
 * monitor:
 *   rate-limit:
 *     capacity: 20
 *     refill-tokens: 20
 *     refill-duration-seconds: 60
 * </pre>
 *
 * @see <a href="https://cwe.mitre.org/data/definitions/307.html">CWE-307: Improper Restriction of Excessive Authentication Attempts</a>
 * @see <a href="https://github.com/bucket4j/bucket4j">Bucket4j documentation</a>
 */
@Component
public class RateLimitingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();
    private final int capacity;
    private final int refillTokens;
    private final Duration refillDuration;

    public RateLimitingFilter(
            @Value("${monitor.rate-limit.capacity:20}") int capacity,
            @Value("${monitor.rate-limit.refill-tokens:20}") int refillTokens,
            @Value("${monitor.rate-limit.refill-duration-seconds:60}") long refillDurationSeconds) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillDuration = Duration.ofSeconds(refillDurationSeconds);

        log.info("Rate limiter initialized: capacity={}, refill={}, duration={}",
                capacity, refillTokens, refillDuration);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = extractClientIp(httpRequest);

        // Only rate-limit authentication-sensitive endpoints
        String path = httpRequest.getRequestURI();
        if (!path.startsWith("/api/events") && !path.startsWith("/api/admin")) {
            chain.doFilter(request, response);
            return;
        }

        Bucket bucket = bucketCache.computeIfAbsent(clientIp, this::createBucket);

        if (bucket.tryConsume(1)) {
            // Request allowed
            long remainingTokens = bucket.getAvailableTokens();
            httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(remainingTokens));
            chain.doFilter(request, response);
        } else {
            // Rate limited
            log.warn("RATE_LIMITED: ip={} path={} method={}",
                    clientIp, path, httpRequest.getMethod());

            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setHeader("Retry-After", String.valueOf(refillDuration.getSeconds()));
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                    "{\"error\":\"rate_limit_exceeded\",\"message\":\"Too many requests. Retry after "
                    + refillDuration.getSeconds() + " seconds.\"}"
            );
        }
    }

    /**
     * Creates a new token bucket for a client IP.
     * Bandwidth: allows `capacity` requests, refilling `refillTokens` every `refillDuration`.
     */
    private Bucket createBucket(String clientIp) {
        log.debug("Creating rate limit bucket for ip={}", clientIp);

        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillTokens, refillDuration)
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Extracts the real client IP, considering X-Forwarded-For for reverse proxies.
     * Falls back to remote address if no proxy header is present.
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For may contain multiple IPs: "client, proxy1, proxy2"
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }
}
