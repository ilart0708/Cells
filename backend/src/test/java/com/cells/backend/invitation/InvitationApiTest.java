package com.cells.backend.invitation;

import com.cells.backend.IntegrationTestBase;
import com.cells.backend.auth.TokenService;
import com.cells.backend.church.Church;
import com.cells.backend.church.ChurchRepository;
import com.cells.backend.common.TokenHasher;
import com.cells.backend.pastor.PastorRepository;
import com.cells.backend.user.Role;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InvitationApiTest extends IntegrationTestBase {

    private static final String ADMIN_URL = "/api/v1/admin/invitations";
    private static final String PASSWORD = "Passw0rd#12345";

    @Autowired private UserRepository users;
    @Autowired private ChurchRepository churches;
    @Autowired private InvitationRepository invitations;
    @Autowired private TokenService tokenService;
    @Autowired private PastorRepository pastors;

    private Church church;
    private User admin;

    @BeforeEach
    void setUp() {
        church = churches.save(new Church("Victory Church"));
        admin = users.save(new User("admin@test.com", "irrelevant", "Test ADMIN", Role.ADMIN));
    }

    // ---------- creating ----------

    @Test
    void adminCreatesAnInvitationAndOnlyTheHashIsStored() throws Exception {
        MvcResult result = invite(" Pastor.John@Test.com ", "Pastor John", church.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PASTOR"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.email").value("pastor.john@test.com"))
                .andExpect(jsonPath("$.churchName").value("Victory Church"))
                .andReturn();

        String url = JsonPath.read(result.getResponse().getContentAsString(), "$.inviteUrl");
        assertThat(url).contains("/accept-invitation#token=");

        String token = tokenOf(result);
        assertThat(invitations.findByTokenHash(token)).isEmpty();                       // raw token not stored
        assertThat(invitations.findByTokenHash(TokenHasher.sha256(token))).isPresent(); // hash is
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"PASTOR", "LEADER", "MEMBER"})
    void nonAdminsCannotManageInvitations(Role role) throws Exception {
        String token = bearer(role);

        mockMvc.perform(get(ADMIN_URL).header("Authorization", token)).andExpect(status().isForbidden());
        mockMvc.perform(post(ADMIN_URL).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteBody("x@test.com", "X", church.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownChurchReturns404() throws Exception {
        invite("a@test.com", "A", 999999L).andExpect(status().isNotFound());
    }

    @Test
    void existingUserEmailIsRejected() throws Exception {
        users.save(new User("taken@test.com", "irrelevant", "Taken", Role.MEMBER));

        invite("taken@test.com", "Taken", church.getId()).andExpect(status().isConflict());
    }

    @Test
    void invalidBodyReturnsFieldErrors() throws Exception {
        invite("not-an-email", "", church.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.fullName").exists());
    }

    @Test
    void aNewInvitationReplacesTheOlderPendingOne() throws Exception {
        String first = createToken("john@test.com");
        String second = createToken("john@test.com");

        preview(first).andExpect(status().isGone());
        preview(second).andExpect(status().isOk());
    }

    // ---------- preview ----------

    @Test
    void previewShowsDetailsAndNeverEchoesTheToken() throws Exception {
        String token = createToken("john@test.com");

        preview(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@test.com"))
                .andExpect(jsonPath("$.fullName").value("Pastor John"))
                .andExpect(jsonPath("$.role").value("PASTOR"))
                .andExpect(jsonPath("$.churchName").value("Victory Church"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void unknownTokenReturns410() throws Exception {
        preview("this-token-does-not-exist").andExpect(status().isGone());
    }

    // ---------- accept ----------

    @Test
    void acceptingCreatesTheAccountLogsTheUserInAndIsSingleUse() throws Exception {
        String token = createToken("john@test.com");
        Long pastorId = users.findByEmail("john@test.com").orElseThrow().getId();
        assertThat(pastors.findById(pastorId)).isPresent();

        MvcResult result = accept(token, "Pastor John", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("PASTOR"))
                .andExpect(jsonPath("$.user.email").value("john@test.com"))
                .andReturn();
        assertThat(result.getResponse().getHeader("Set-Cookie"))
                .startsWith("cells_refresh=").contains("HttpOnly");

        // the chosen password works for a normal login
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"john@test.com\",\"password\":\"%s\"}".formatted(PASSWORD)))
                .andExpect(status().isOk());

        // the link cannot be used twice
        accept(token, "Pastor John", PASSWORD).andExpect(status().isGone());
        preview(token).andExpect(status().isGone());
    }

    @Test
    void expiredInvitationCannotBeUsed() throws Exception {
        invitations.save(new Invitation(TokenHasher.sha256("expired-token"), "late@test.com", "Late",
                Role.PASTOR, church, admin.getId(), Instant.now().minusSeconds(60)));

        preview("expired-token").andExpect(status().isGone());
        accept("expired-token", "Late", PASSWORD).andExpect(status().isGone());
        assertThat(users.existsByEmail("late@test.com")).isFalse();
    }

    @Test
    void shortPasswordIsRejectedAndTheLinkStaysUsable() throws Exception {
        String token = createToken("john@test.com");

        accept(token, "Pastor John", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        accept(token, "Pastor John", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void concurrentAcceptsOfOneLinkSucceedOnlyOnce() throws Exception {
        String token = createToken("race@test.com");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return accept(token, "Race Pastor", PASSWORD).andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> future : futures) {
            statuses.add(future.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(statuses).containsExactlyInAnyOrder(200, 410);
        assertThat(users.findByEmail("race@test.com")).isPresent();
    }

    // ---------- list and revoke ----------

    @Test
    void adminCanListAndRevoke() throws Exception {
        String token = createToken("john@test.com");

        mockMvc.perform(get(ADMIN_URL).header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].inviteUrl").doesNotExist());

        Long id = invitations.findByTokenHash(TokenHasher.sha256(token)).orElseThrow().getId();
        mockMvc.perform(delete(ADMIN_URL + "/" + id).header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isNoContent());

        preview(token).andExpect(status().isGone());
    }

    @Test
    void anAcceptedInvitationCannotBeRevoked() throws Exception {
        String token = createToken("john@test.com");
        accept(token, "Pastor John", PASSWORD).andExpect(status().isOk());
        Long id = invitations.findByTokenHash(TokenHasher.sha256(token)).orElseThrow().getId();

        mockMvc.perform(delete(ADMIN_URL + "/" + id).header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---------- helpers ----------

    private ResultActions invite(String email, String fullName, Long churchId) throws Exception {
        return mockMvc.perform(post(ADMIN_URL).header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(inviteBody(email, fullName, churchId)));
    }

    private String createToken(String email) throws Exception {
        return tokenOf(invite(email, "Pastor John", church.getId()).andExpect(status().isCreated()).andReturn());
    }

    private ResultActions preview(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/invitations/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\"}".formatted(token)));
    }

    private ResultActions accept(String token, String fullName, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/invitations/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"fullName\":\"%s\",\"password\":\"%s\"}"
                        .formatted(token, fullName, password)));
    }

    private static String tokenOf(MvcResult result) throws Exception {
        String url = JsonPath.read(result.getResponse().getContentAsString(), "$.inviteUrl");
        return url.substring(url.indexOf("token=") + "token=".length());
    }

    private static String inviteBody(String email, String fullName, Long churchId) {
        return "{\"email\":\"%s\",\"fullName\":\"%s\",\"churchId\":%d}".formatted(email, fullName, churchId);
    }

    private String bearer(Role role) {
        String email = role.name().toLowerCase() + "@test.com";
        User user = users.findByEmail(email)
                .orElseGet(() -> users.save(new User(email, "irrelevant", "Test " + role, role)));
        return "Bearer " + tokenService.createAccessToken(user).value();
    }
}