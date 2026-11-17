package com.cells.backend.pastor;

import com.cells.backend.IntegrationTestBase;
import com.cells.backend.auth.TokenService;
import com.cells.backend.church.Church;
import com.cells.backend.church.ChurchRepository;
import com.cells.backend.user.Role;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PastorApiTest extends IntegrationTestBase {

    private static final String URL = "/api/v1/admin/pastors";
    private static final String PASSWORD = "Passw0rd#12345";

    @Autowired private UserRepository users;
    @Autowired private ChurchRepository churches;
    @Autowired private PastorRepository pastors;
    @Autowired private TokenService tokenService;
    @Autowired private PasswordEncoder encoder;

    private Church victory;
    private Church grace;
    private User john;

    @BeforeEach
    void setUp() {
        victory = churches.save(new Church("Victory Church"));
        grace = churches.save(new Church("Grace Community Church"));
        john = pastor("john@test.com", "Pastor John", victory);
        pastor("mary@test.com", "Pastor Mary", grace);
    }

    @Test
    void adminListsPastorsWithTheirChurch() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].fullName").value("Pastor John"))
                .andExpect(jsonPath("$.content[0].churchName").value("Victory Church"))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    @Test
    void searchMatchesNameEmailAndChurch() throws Exception {
        String token = bearer(Role.ADMIN);

        mockMvc.perform(get(URL).param("q", "MARY").header("Authorization", token))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get(URL).param("q", "john@test").header("Authorization", token))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get(URL).param("q", "grace community").header("Authorization", token))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Pastor Mary"));
        mockMvc.perform(get(URL).param("q", "nobody").header("Authorization", token))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"PASTOR", "LEADER", "MEMBER"})
    void nonAdminsAreForbidden(Role role) throws Exception {
        String token = bearer(role);

        mockMvc.perform(get(URL).header("Authorization", token)).andExpect(status().isForbidden());
        mockMvc.perform(patch(URL + "/" + john.getId() + "/status").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOneAndUnknownIds() throws Exception {
        String token = bearer(Role.ADMIN);
        Long adminId = users.findByEmail("admin@test.com").orElseThrow().getId();

        mockMvc.perform(get(URL + "/" + john.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.churchId").value(victory.getId()));
        mockMvc.perform(get(URL + "/999999").header("Authorization", token))
                .andExpect(status().isNotFound());
        // a user who exists but is not a pastor must not be reachable through this endpoint
        mockMvc.perform(get(URL + "/" + adminId).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void disablingAPastorBlocksLoginAndEnablingRestoresIt() throws Exception {
        String token = bearer(Role.ADMIN);

        mockMvc.perform(patch(URL + "/" + john.getId() + "/status").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
        login("john@test.com").andExpect(status().isForbidden());

        mockMvc.perform(patch(URL + "/" + john.getId() + "/status").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        login("john@test.com").andExpect(status().isOk());
    }

    @Test
    void pendingIsNotAValidManualStatusAndUnknownValuesAreRejected() throws Exception {
        String token = bearer(Role.ADMIN);

        mockMvc.perform(patch(URL + "/" + john.getId() + "/status").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(patch(URL + "/" + john.getId() + "/status").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"BANANA\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(URL + "/" + john.getId() + "/status").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- helpers ----------

    private User pastor(String email, String fullName, Church church) {
        User user = users.save(new User(email, encoder.encode(PASSWORD), fullName, Role.PASTOR));
        pastors.save(new Pastor(user.getId(), church));
        return user;
    }

    private org.springframework.test.web.servlet.ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)));
    }

    private String bearer(Role role) {
        String email = role.name().toLowerCase() + "@test.com";
        User user = users.findByEmail(email)
                .orElseGet(() -> users.save(new User(email, "irrelevant", "Test " + role, role)));
        return "Bearer " + tokenService.createAccessToken(user).value();
    }
}