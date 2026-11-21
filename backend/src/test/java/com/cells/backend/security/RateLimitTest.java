package com.cells.backend.security;

import com.cells.backend.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.max-requests=3",
        "app.rate-limit.window=1m"
})
class RateLimitTest extends IntegrationTestBase {

    @Autowired MockMvc rateMvc;
    @Autowired RateLimitFilter filter;

    private static final String BAD_LOGIN =
            "{\"email\":\"nobody@example.com\",\"password\":\"wrong-password-123\"}";

    @BeforeEach
    void clear() {
        filter.reset();
    }

    @Test
    void fourthLoginAttemptInAMinuteIsBlockedWith429() throws Exception {
        for (int i = 0; i < 3; i++) {
            rateMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON).content(BAD_LOGIN))
                    .andExpect(status().isUnauthorized());
        }
        rateMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(BAD_LOGIN))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void invitationAcceptHasItsOwnCounter() throws Exception {
        for (int i = 0; i < 4; i++) {
            rateMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON).content(BAD_LOGIN));
        }
        rateMvc.perform(post("/api/v1/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"x\",\"fullName\":\"A B\",\"password\":\"long-enough-pass-1\"}"))
                .andExpect(status().is(org.hamcrest.Matchers.not(429)));
    }

    @Test
    void otherEndpointsAreNotLimited() throws Exception {
        for (int i = 0; i < 6; i++) {
            rateMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        }
    }
}