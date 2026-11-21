package com.cells.backend.security;

import java.io.IOException;
import java.time.Clock;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.cells.backend.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(prefix = "app.rate-limit", name = "enabled", havingValue = "true")
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS =
            Set.of("/api/v1/auth/login", "/api/v1/invitations/accept");
    private static final int PURGE_THRESHOLD = 10_000;

    private record Window(long startedAtMs, int count) {
    }

    private final RateLimitProperties props;
    private final Clock clock = Clock.systemUTC();
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
                || !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long now = clock.millis();
        long windowMs = props.window().toMillis();
        String key = clientIp(request) + "|" + request.getRequestURI();

        Window w = windows.compute(key, (k, old) ->
                (old == null || now - old.startedAtMs() >= windowMs)
                        ? new Window(now, 1)
                        : new Window(old.startedAtMs(), old.count() + 1));

        if (windows.size() > PURGE_THRESHOLD) {
            windows.values().removeIf(v -> now - v.startedAtMs() >= windowMs);
        }

        if (w.count() > props.maxRequests()) {
            long retryAfter = Math.max(1, (w.startedAtMs() + windowMs - now + 999) / 1000);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"title\":\"Too Many Requests\",\"status\":429,"
                    + "\"detail\":\"Too many attempts. Please wait " + retryAfter
                    + " seconds and try again.\",\"instance\":\"" + request.getRequestURI() + "\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String headerName = props.clientIpHeader();
        if (headerName != null && !headerName.isBlank()) {
            String value = request.getHeader(headerName);
            if (value != null && !value.isBlank()) {
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /** For tests. */
    public void reset() {
        windows.clear();
    }
}