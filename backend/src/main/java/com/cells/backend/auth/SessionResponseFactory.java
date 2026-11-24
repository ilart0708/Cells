package com.cells.backend.auth;

import com.cells.backend.config.CorsProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Decides HOW a new session reaches the client. The website gets the refresh token as an httpOnly cookie, as
 * always. The native app announces itself with the header "X-Client-Type: native" and gets the refresh token in
 * the response body instead, because a WebView at https://localhost cannot use a SameSite=Strict cookie that
 * belongs to the server's domain.
 *
 * Native mode is only honored for the app's own origins (app.cors.allowed-origins). A page of the website could
 * otherwise send the same header and read the long-lived refresh token, which would defeat the httpOnly cookie.
 * Browsers always attach an Origin header to a POST and a page cannot change it. Tools such as Postman or curl send
 * no Origin at all, so they can use native mode for testing; a script in a browser cannot.
 */
@Component
public class SessionResponseFactory {

    public static final String CLIENT_HEADER = "X-Client-Type";
    public static final String ORIGIN_HEADER = "Origin";
    private static final String NATIVE = "native";

    private final RefreshCookieFactory cookieFactory;
    private final List<String> appOrigins;

    public SessionResponseFactory(RefreshCookieFactory cookieFactory, CorsProperties corsProperties) {
        this.cookieFactory = cookieFactory;
        this.appOrigins = corsProperties.allowedOriginsOrEmpty();
    }

    public boolean isNativeClient(String clientType, String origin) {
        if (clientType == null || !NATIVE.equalsIgnoreCase(clientType.trim())) {
            return false;
        }
        return origin == null || origin.isBlank() || appOrigins.contains(origin.trim());
    }

    /** The token to refresh or revoke: from the body for the native app, from the cookie for the website. */
    public String refreshTokenFrom(String clientType, String origin, String cookieToken, RefreshRequest body) {
        if (isNativeClient(clientType, origin) && body != null && body.refreshToken() != null
                && !body.refreshToken().isBlank()) {
            return body.refreshToken();
        }
        return cookieToken;
    }

    public ResponseEntity<?> create(AuthResult result, String clientType, String origin) {
        if (isNativeClient(clientType, origin)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(NativeSessionResponse.of(result.body(), result.refreshToken()));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.create(result.refreshToken()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(result.body());
    }

    public ResponseEntity<Void> loggedOut() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                .build();
    }
}
