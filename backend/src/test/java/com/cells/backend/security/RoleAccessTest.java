package com.cells.backend.security;

import com.cells.backend.IntegrationTestBase;
import com.cells.backend.auth.RefreshTokenRepository;
import com.cells.backend.auth.TokenService;
import com.cells.backend.user.Role;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleAccessTest extends IntegrationTestBase {

    private static final List<String> AREAS = List.of("admin", "pastor", "leader", "member");

    @Autowired private UserRepository users;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private TokenService tokenService;
    @Autowired private JwtEncoder jwtEncoder;

    @BeforeEach
    void clean() {
        refreshTokens.deleteAll();
        users.deleteAll();
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void eachRoleReachesOnlyItsOwnArea(Role role) throws Exception {
        String token = tokenFor(role);

        mockMvc.perform(get("/api/v1/probe/any").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        for (String area : AREAS) {
            int expected = area.equalsIgnoreCase(role.name()) ? 200 : 403;
            int actual = mockMvc.perform(get("/api/v1/probe/" + area)
                            .header("Authorization", "Bearer " + token))
                    .andReturn().getResponse().getStatus();
            assertThat(actual).as("%s calling /probe/%s", role, area).isEqualTo(expected);
        }
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void forbiddenResponseIsJson(Role role) throws Exception {
        String otherArea = role == Role.ADMIN ? "pastor" : "admin";

        mockMvc.perform(get("/api/v1/probe/" + otherArea)
                        .header("Authorization", "Bearer " + tokenFor(role)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void noTokenReturns401OnEveryArea() throws Exception {
        for (String area : List.of("any", "admin", "pastor", "leader", "member")) {
            mockMvc.perform(get("/api/v1/probe/" + area))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.title").value("Unauthorized"));
        }
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String token = tokenFor(Role.ADMIN);

        mockMvc.perform(get("/api/v1/probe/admin").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        User admin = users.save(new User("expired@test.com", "irrelevant", "Expired", Role.ADMIN));
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cells")
                .issuedAt(now.minusSeconds(3600))
                .expiresAt(now.minusSeconds(1800))
                .subject(String.valueOf(admin.getId()))
                .claim("roles", List.of("ADMIN"))
                .build();
        String expired = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mockMvc.perform(get("/api/v1/probe/admin").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithWrongIssuerIsRejected() throws Exception {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("someone-else")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(600))
                .subject("1")
                .claim("roles", List.of("ADMIN"))
                .build();
        String forged = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mockMvc.perform(get("/api/v1/probe/admin").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    private String tokenFor(Role role) {
        User user = users.save(new User(
                role.name().toLowerCase() + "@test.com", "irrelevant", "Test " + role, role));
        return tokenService.createAccessToken(user).value();
    }
}