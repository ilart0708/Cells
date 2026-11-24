package com.cells.backend.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.cells.backend.config.RateLimitProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final Set<String> LIMITED_PATHS =
            Set.of("/api/v1/auth/login", "/api/v1/invitations/accept");
    private static final int PURGE_THRESHOLD = 10_000;

    private record Window(long startedAtMs, int count) {
    }

    private final RateLimitProperties props;
    private final CorsConfigurationSource corsSource;
    private final CorsProcessor corsProcessor = new DefaultCorsProcessor();
    private final Clock clock = Clock.systemUTC();
    private final ObjectMapper json = new ObjectMapper();
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

        if (props.debug()) {
            // Only while app.rate-limit.debug is true (APP_RATE_LIMIT_DEBUG on Render). Addresses are personal data:
            // turn it off again as soon as the right header is known. No request body or credentials are logged.
            log.info("Rate-limit debug: key={} remoteAddr={} x-forwarded-for={} cf-connecting-ip={} true-client-ip={} "
                            + "x-nf-client-connection-ip={} x-real-ip={}",
                    key, request.getRemoteAddr(), request.getHeader("X-Forwarded-For"),
                    request.getHeader("CF-Connecting-IP"), request.getHeader("True-Client-IP"),
                    request.getHeader("X-Nf-Client-Connection-Ip"), request.getHeader("X-Real-Ip"));
        }

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
        String netlifyIp = verifiedNetlifyClientIp(request);
        if (netlifyIp != null) {
            return netlifyIp;
        }
        String headerName = props.clientIpHeader();
        if (headerName != null && !headerName.isBlank()) {
            String value = request.getHeader(headerName);
            if (value != null && !value.isBlank()) {
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Website path: Netlify proxies /api/* to this backend and, for a "signed" redirect, adds an x-nf-sign header (a JWS,
     * HMAC HS256, claims iss=netlify and exp). Only when that signature verifies with our shared secret is the visitor
     * address in X-Nf-Client-Connection-Ip trusted; anyone else could write that header themselves. Returns null when
     * the secret is not configured or anything about the signature is wrong, so callers fall back to the normal address.
     */
    private String verifiedNetlifyClientIp(HttpServletRequest request) {
        String secret = props.netlifySignSecret();
        String token = request.getHeader("x-nf-sign");
        if (secret == null || secret.isBlank() || token == null || token.isBlank()) {
            return null;
        }
        try {
            String[] parts = token.trim().split("\\.");
            if (parts.length != 3) {
                return null;
            }
            Base64.Decoder decoder = Base64.getUrlDecoder();
            JsonNode header = json.readTree(decoder.decode(parts[0]));
            if (!"HS256".equals(header.path("alg").asText())) {
                return null; // never accept "none" or another algorithm
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!MessageDigest.isEqual(expected, decoder.decode(parts[2]))) {
                return null;
            }
            JsonNode claims = json.readTree(decoder.decode(parts[1]));
            if (!"netlify".equals(claims.path("iss").asText())) {
                return null;
            }
            long exp = claims.path("exp").asLong(0);
            if (exp == 0 || exp * 1000 < clock.millis()) {
                return null;
            }
            String ip = request.getHeader("X-Nf-Client-Connection-Ip");
            return (ip == null || ip.isBlank()) ? null : ip.trim();
        } catch (Exception e) {
            return null; // malformed token: treat as unsigned
        }
    }

    /** For tests. */
    public void reset() {
        windows.clear();
    }
}