package com.cells.backend.auth;

import com.cells.backend.IntegrationTestBase;
import com.cells.backend.user.Role;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowTest extends IntegrationTestBase {

    private static final String EMAIL = "admin@test.com";
    private static final String PASSWORD = "Passw0rd#12345";

    @Autowired private UserRepository users;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private PasswordEncoder encoder;

    @BeforeEach
    void clean() {
        refreshTokens.deleteAll();
        users.deleteAll();
    }

    // ---------- login ----------

    @Test
    void loginReturnsTokenUserAndSecureRefreshCookie() throws Exception {
        createUser(EMAIL, Role.ADMIN, false);

        MvcResult result = login(EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie)
                .startsWith("cells_refresh=")
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .contains("Path=/api/v1/auth");
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveIdenticalResponses() throws Exception {
        createUser(EMAIL, Role.ADMIN, false);

        MvcResult wrongPassword = login(EMAIL, "WrongPassword#1").andExpect(status().isUnauthorized()).andReturn();
        MvcResult unknownEmail = login("nobody@test.com", "WrongPassword#1").andExpect(status().isUnauthorized()).andReturn();

        assertThat(unknownEmail.getResponse().getContentAsString())
                .isEqualTo(wrongPassword.getResponse().getContentAsString());
    }

    @Test
    void loginWithInvalidBodyReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nope\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Malformed request body."));
    }

    @Test
    void disabledUserCannotLogIn() throws Exception {
        createUser(EMAIL, Role.MEMBER, true);
        login(EMAIL, PASSWORD).andExpect(status().isForbidden());
    }

    // ---------- /me ----------

    @Test
    void meReturnsCurrentUserWithValidToken() throws Exception {
        createUser(EMAIL, Role.LEADER, false);
        String token = accessTokenOf(login(EMAIL, PASSWORD).andReturn());

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value("LEADER"));
    }

    @Test
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    // ---------- refresh ----------

    @Test
    void refreshRotatesTheTokenAndIssuesANewAccessToken() throws Exception {
        createUser(EMAIL, Role.ADMIN, false);
        String first = refreshCookieOf(login(EMAIL, PASSWORD).andReturn());

        MvcResult refreshed = refresh(first).andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty()).andReturn();

        assertThat(refreshCookieOf(refreshed)).isNotEqualTo(first);
    }

    @Test
    void reusingAnOldRefreshTokenRevokesTheWholeFamily() throws Exception {
        createUser(EMAIL, Role.ADMIN, false);
        String first = refreshCookieOf(login(EMAIL, PASSWORD).andReturn());
        String second = refreshCookieOf(refresh(first).andExpect(status().isOk()).andReturn());

        refresh(first).andExpect(status().isUnauthorized());   // replay of a used token
        refresh(second).andExpect(status().isUnauthorized());  // the family is now dead too
    }

    @Test
    void refreshWithoutCookieReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshFailsAfterUserIsDisabled() throws Exception {
        User user = createUser(EMAIL, Role.MEMBER, false);
        String cookie = refreshCookieOf(login(EMAIL, PASSWORD).andReturn());

        user.disable();
        users.save(user);

        refresh(cookie).andExpect(status().isForbidden());
        refresh(cookie).andExpect(status().isUnauthorized()); // family was revoked
    }

    // ---------- logout ----------

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        createUser(EMAIL, Role.ADMIN, false);
        String cookie = refreshCookieOf(login(EMAIL, PASSWORD).andReturn());

        mockMvc.perform(post("/api/v1/auth/logout").cookie(new Cookie("cells_refresh", cookie)))
                .andExpect(status().isNoContent());

        refresh(cookie).andExpect(status().isUnauthorized());
    }

    // ---------- helpers ----------

    private User createUser(String email, Role role, boolean disabled) {
        User user = new User(email, encoder.encode(PASSWORD), "Test " + role, role);
        if (disabled) {
            user.disable();
        }
        return users.save(user);
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String cookieValue) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("cells_refresh", cookieValue)));
    }

    private static String accessTokenOf(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static String refreshCookieOf(MvcResult result) {
        String header = result.getResponse().getHeader("Set-Cookie");
        assertThat(header).as("Set-Cookie header").isNotNull();
        return header.substring("cells_refresh=".length(), header.indexOf(';'));
    }
}