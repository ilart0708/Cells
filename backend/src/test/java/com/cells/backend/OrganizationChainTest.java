package com.cells.backend;

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
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Builds the whole hierarchy through the public API, exactly as the real people would,
 * then checks what each of them can and cannot see.
 */
class OrganizationChainTest extends IntegrationTestBase {

    private static final String PASSWORD = "Passw0rd#12345";

    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder encoder;

    /** A logged-in person: their access token, refresh cookie and user id. */
    private record Session(String accessToken, String refreshCookie, long userId) {
        String bearer() {
            return "Bearer " + accessToken;
        }
    }

    private String admin;      // bearer header of the administrator
    private Session john;      // pastor at Victory Church
    private Session mary;      // pastor at Grace Community Church
    private Session amy;       // leader under John
    private Session ben;       // leader under Mary

    @BeforeEach
    void buildTheWorld() throws Exception {
        users.save(new User("admin@chain.test", encoder.encode(PASSWORD), "Chain Admin", Role.ADMIN));
        admin = "Bearer " + JsonPath.<String>read(
                login("admin@chain.test", PASSWORD).andReturn().getResponse().getContentAsString(),
                "$.accessToken");

        long victory = createChurch("Victory Church");
        long grace = createChurch("Grace Community Church");

        john = acceptInvitation(
                send("/api/v1/admin/invitations", admin,
                        "{\"email\":\"john@chain.test\",\"fullName\":\"Pastor John\",\"churchId\":%d}".formatted(victory)),
                "Pastor John");
        mary = acceptInvitation(
                send("/api/v1/admin/invitations", admin,
                        "{\"email\":\"mary@chain.test\",\"fullName\":\"Pastor Mary\",\"churchId\":%d}".formatted(grace)),
                "Pastor Mary");

        amy = acceptInvitation(
                send("/api/v1/pastor/invitations", john.bearer(),
                        "{\"email\":\"amy@chain.test\",\"fullName\":\"Leader Amy\"}"),
                "Leader Amy");
        ben = acceptInvitation(
                send("/api/v1/pastor/invitations", mary.bearer(),
                        "{\"email\":\"ben@chain.test\",\"fullName\":\"Leader Ben\"}"),
                "Leader Ben");
    }

    // ---------- identities ----------

    @Test
    void everyoneEndsUpWithTheirOwnRole() throws Exception {
        expectRole(john, "PASTOR");
        expectRole(mary, "PASTOR");
        expectRole(amy, "LEADER");
        expectRole(ben, "LEADER");
    }

    // ---------- what the administrator sees ----------

    @Test
    void theAdministratorSeesTheWholeHierarchy() throws Exception {
        mockMvc.perform(get("/api/v1/admin/pastors").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.fullName=='Pastor John')].churchName").value("Victory Church"))
                .andExpect(jsonPath("$.content[?(@.fullName=='Pastor Mary')].churchName").value("Grace Community Church"));

        mockMvc.perform(get("/api/v1/admin/leaders").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.fullName=='Leader Amy')].pastorName").value("Pastor John"))
                .andExpect(jsonPath("$.content[?(@.fullName=='Leader Ben')].pastorName").value("Pastor Mary"))
                .andExpect(jsonPath("$.content[?(@.fullName=='Leader Amy')].churchName").value("Victory Church"));

        mockMvc.perform(get("/api/v1/admin/invitations").header("Authorization", admin))
                .andExpect(jsonPath("$.content[?(@.status=='PENDING')]").isEmpty());
    }

    // ---------- what each pastor sees ----------

    @Test
    void eachPastorSeesOnlyTheirOwnLeaders() throws Exception {
        mockMvc.perform(get("/api/v1/pastor/leaders").header("Authorization", john.bearer()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Leader Amy"));

        mockMvc.perform(get("/api/v1/pastor/leaders").header("Authorization", mary.bearer()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Leader Ben"));
    }

    @Test
    void aPastorCannotReachAnotherPastorsLeaderEvenByGuessingTheId() throws Exception {
        mockMvc.perform(get("/api/v1/pastor/leaders/" + ben.userId()).header("Authorization", john.bearer()))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/pastor/leaders/" + ben.userId() + "/status")
                        .header("Authorization", john.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isNotFound());

        login("ben@chain.test", PASSWORD).andExpect(status().isOk());
    }

    // ---------- area boundaries across the whole chain ----------

    @Test
    void everyoneStaysInTheirOwnArea() throws Exception {
        // administrator area: only the administrator
        for (Session s : new Session[]{john, mary, amy, ben}) {
            mockMvc.perform(get("/api/v1/admin/churches").header("Authorization", s.bearer()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/admin/pastors").header("Authorization", s.bearer()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/admin/leaders").header("Authorization", s.bearer()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/admin/invitations").header("Authorization", s.bearer()))
                    .andExpect(status().isForbidden());
        }

        // pastor area: only pastors (not even the administrator)
        mockMvc.perform(get("/api/v1/pastor/leaders").header("Authorization", admin))
                .andExpect(status().isForbidden());
        for (Session leader : new Session[]{amy, ben}) {
            mockMvc.perform(get("/api/v1/pastor/leaders").header("Authorization", leader.bearer()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/pastor/invitations").header("Authorization", leader.bearer()))
                    .andExpect(status().isForbidden());
        }
    }

    // ---------- disabling ends sessions that already exist ----------

    @Test
    void disablingALeaderEndsTheirSessionAtTheNextRefresh() throws Exception {
        mockMvc.perform(patch("/api/v1/pastor/leaders/" + amy.userId() + "/status")
                        .header("Authorization", john.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk());

        refresh(amy).andExpect(status().isForbidden());
        login("amy@chain.test", PASSWORD).andExpect(status().isForbidden());
    }

    @Test
    void disablingAPastorEndsTheirSessionAtTheNextRefresh() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/pastors/" + john.userId() + "/status")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk());

        refresh(john).andExpect(status().isForbidden());
        login("john@chain.test", PASSWORD).andExpect(status().isForbidden());

        // the other pastor is unaffected
        refresh(mary).andExpect(status().isOk());
    }

    // ---------- helpers ----------

    private long createChurch(String name) throws Exception {
        MvcResult result = send("/api/v1/admin/churches", admin, "{\"name\":\"%s\"}".formatted(name));
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    /** POST with a bearer token; the call must return 201 Created. */
    private MvcResult send(String url, String bearer, String body) throws Exception {
        return mockMvc.perform(post(url).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
    }

    /** Opens the invitation link from the creation response and sets a password, as the invitee would. */
    private Session acceptInvitation(MvcResult invitation, String fullName) throws Exception {
        String url = JsonPath.read(invitation.getResponse().getContentAsString(), "$.inviteUrl");
        String token = url.substring(url.indexOf("token=") + "token=".length());

        MvcResult accepted = mockMvc.perform(post("/api/v1/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"fullName\":\"%s\",\"password\":\"%s\"}"
                                .formatted(token, fullName, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        String body = accepted.getResponse().getContentAsString();
        String setCookie = accepted.getResponse().getHeader("Set-Cookie");
        return new Session(
                JsonPath.read(body, "$.accessToken"),
                setCookie.substring("cells_refresh=".length(), setCookie.indexOf(';')),
                ((Number) JsonPath.read(body, "$.user.id")).longValue());
    }

    private void expectRole(Session session, String role) throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(role));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private ResultActions refresh(Session session) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(new Cookie("cells_refresh", session.refreshCookie())));
    }
}