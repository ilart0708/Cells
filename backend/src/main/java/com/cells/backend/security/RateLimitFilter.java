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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.DefaultCorsProcessor;
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
    private final CorsConfigurationSource corsSource;
    private final CorsProcessor corsProcessor = new DefaultCorsProcessor();
    private final Clock clock = Clock.systemUTC();
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties props,
                           @Qualifier("corsConfigurationSource") CorsConfigurationSource corsSource) {
        this.props = props;
        this.corsSource = corsSource;
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
            // This filter runs BEFORE Spring Security's CORS filter, so without this the app (a different origin)
            // would receive a 429 with no CORS headers and the WebView would report an opaque network error
            // instead of "Too many attempts. Please wait N seconds". The website (same origin) never noticed.
            CorsConfiguration cors = corsSource.getCorsConfiguration(request);
            if (cors != null && !corsProcessor.processRequest(cors, request, response)) {
                return; // an origin that is not allowed: the CORS processor already answered 403
            }
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