package com.zorvyn.financedataprocessing.security;

import com.zorvyn.financedataprocessing.exception.ApiErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, RateLimitWindow> windows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final Duration windowDuration;
    private final ApiErrorWriter apiErrorWriter;

    public RateLimitingFilter(
            ApiErrorWriter apiErrorWriter,
            @Value("${app.rate-limit.max-requests:60}") int maxRequests,
            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds
    ) {
        this.apiErrorWriter = apiErrorWriter;
        this.maxRequests = maxRequests;
        this.windowDuration = Duration.ofSeconds(windowSeconds);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/h2-console") || path.equals("/api/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientKey = request.getRemoteAddr();
        Instant now = Instant.now();
        RateLimitWindow window = windows.compute(clientKey, (key, existing) -> refreshWindow(existing, now));

        if (window.requestCount() >= maxRequests) {
            apiErrorWriter.write(response, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Please retry later.", List.of());
            return;
        }

        window.increment();
        filterChain.doFilter(request, response);
    }

    private RateLimitWindow refreshWindow(RateLimitWindow existing, Instant now) {
        if (existing == null || now.isAfter(existing.windowStartedAt().plus(windowDuration))) {
            return new RateLimitWindow(now);
        }
        return existing;
    }

    private static final class RateLimitWindow {
        private final Instant windowStartedAt;
        private int requestCount;

        private RateLimitWindow(Instant windowStartedAt) {
            this.windowStartedAt = windowStartedAt;
            this.requestCount = 0;
        }

        private Instant windowStartedAt() {
            return windowStartedAt;
        }

        private int requestCount() {
            return requestCount;
        }

        private void increment() {
            requestCount++;
        }
    }
}
