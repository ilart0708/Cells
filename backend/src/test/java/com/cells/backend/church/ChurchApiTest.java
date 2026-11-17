package com.cells.backend.church;

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
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChurchApiTest extends IntegrationTestBase {

    private static final String URL = "/api/v1/admin/churches";

    @Autowired private UserRepository users;
    @Autowired private RefreshTokenRepository refreshTokens;
    @Autowired private ChurchRepository churches;
    @Autowired private TokenService tokenService;

    @BeforeEach
    void clean() {
        refreshTokens.deleteAll();
        users.deleteAll();
        churches.deleteAll();
    }

    @Test
    void adminCreatesAChurch() throws Exception {
        mockMvc.perform(post(URL).header("Authorization", bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Victory Church")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Victory Church"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void duplicateNameIgnoringCaseAndSpacesIsRejected() throws Exception {
        churches.save(new Church("Victory Church"));

        mockMvc.perform(post(URL).header("Authorization", bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("  victory church ")))
                .andExpect(status().isConflict());
    }

    @Test
    void blankNameIsRejectedWithFieldErrors() throws Exception {
        mockMvc.perform(post(URL).header("Authorization", bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"PASTOR", "LEADER", "MEMBER"})
    void nonAdminsAreForbidden(Role role) throws Exception {
        String token = bearer(role);

        mockMvc.perform(get(URL).header("Authorization", token)).andExpect(status().isForbidden());
        mockMvc.perform(post(URL).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Grace Church")))
                .andExpect(status().isForbidden());
    }

    @Test
    void noTokenReturns401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void listIsSearchableAndPageSizeIsCapped() throws Exception {
        churches.save(new Church("Victory Church"));
        churches.save(new Church("Grace Community Church"));
        churches.save(new Church("Hope Chapel"));
        String token = bearer(Role.ADMIN);

        mockMvc.perform(get(URL).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].name").value("Grace Community Church"));

        mockMvc.perform(get(URL).param("q", "GRACE").header("Authorization", token))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get(URL).param("size", "1000").header("Authorization", token))
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void getUnknownChurchReturns404() throws Exception {
        mockMvc.perform(get(URL + "/999999").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void renameWorksAndCanChangeOnlyTheCaseOfItsOwnName() throws Exception {
        Church church = churches.save(new Church("Victory Church"));
        String token = bearer(Role.ADMIN);

        mockMvc.perform(put(URL + "/" + church.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("VICTORY CHURCH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("VICTORY CHURCH"));
    }

    @Test
    void renameToAnotherChurchesNameIsRejected() throws Exception {
        churches.save(new Church("Grace Community Church"));
        Church victory = churches.save(new Church("Victory Church"));

        mockMvc.perform(put(URL + "/" + victory.getId()).header("Authorization", bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("grace community church")))
                .andExpect(status().isConflict());
    }

    private String bearer(Role role) {
        User user = users.save(new User(
                role.name().toLowerCase() + "@test.com", "irrelevant", "Test " + role, role));
        return "Bearer " + tokenService.createAccessToken(user).value();
    }

    private static String body(String name) {
        return "{\"name\":\"%s\"}".formatted(name);
    }
}