package com.cells.backend.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Decides HOW a new session reaches the client. The website gets the refresh token as an httpOnly cookie, as
 * always. The native app announces itself with the header "X-Client-Type: native" and gets the refresh token in
 * the response body instead, because a WebView at https://localhost cannot use a SameSite=Strict cookie that
 * belongs to the server's domain. Every other value behaves exactly like before.
 */
@Component
public class SessionResponseFactory {

    public static final String CLIENT_HEADER = "X-Client-Type";
    private static final String NATIVE = "native";

    private final RefreshCookieFactory cookieFactory;

    public SessionResponseFactory(RefreshCookieFactory cookieFactory) {
        this.cookieFactory = cookieFactory;
    }

    public static boolean isNative(String clientType) {
        return clientType != null && NATIVE.equalsIgnoreCase(clientType.trim());
    }

    /** The token to refresh or revoke: from the body for the native app, from the cookie for the website. */
    public static String refreshTokenFrom(String clientType, String cookieToken, RefreshRequest body) {
        if (isNative(clientType) && body != null && body.refreshToken() != null && !body.refreshToken().isBlank()) {
            return body.refreshToken();
        }
        return cookieToken;
    }

    public ResponseEntity<?> create(AuthResult result, String clientType) {
        if (isNative(clientType)) {
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
