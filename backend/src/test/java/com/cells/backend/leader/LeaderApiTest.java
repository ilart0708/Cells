package com.cells.backend.leader;

import com.cells.backend.IntegrationTestBase;
import com.cells.backend.auth.TokenService;
import com.cells.backend.church.Church;
import com.cells.backend.church.ChurchRepository;
import com.cells.backend.pastor.Pastor;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeaderApiTest extends IntegrationTestBase {

    private static final String PASTOR_URL = "/api/v1/pastor";
    private static final String ADMIN_URL = "/api/v1/admin/leaders";
    private static final String PASSWORD = "Passw0rd#12345";

    @Autowired private UserRepository users;
    @Autowired private ChurchRepository churches;
    @Autowired private PastorRepository pastors;
    @Autowired private CellGroupLeaderRepository leaders;
    @Autowired private PastorLeaderRepository pastorLeaders;
    @Autowired private TokenService tokenService;
    @Autowired private PasswordEncoder encoder;

    private Church victory;
    private Church grace;
    private User john;      // pastor at Victory
    private User mary;      // pastor at Grace
    private User leaderOfJohn;
    private User leaderOfMary;

    @BeforeEach
    void setUp() {
        victory = churches.save(new Church("Victory Church"));
        grace = churches.save(new Church("Grace Community Church"));
        john = pastor("john@test.com", "Pastor John", victory);
        mary = pastor("mary@test.com", "Pastor Mary", grace);
        leaderOfJohn = leader("amy@test.com", "Leader Amy", victory, john);
        leaderOfMary = leader("ben@test.com", "Leader Ben", grace, mary);
    }

    // ---------- inviting ----------

    @Test
    void pastorInvitesALeaderIntoTheirOwnChurchEvenIfTheyAskForAnotherOne() throws Exception {
        // the request carries a churchId for Grace; the server must ignore it
        MvcResult result = mockMvc.perform(post(PASTOR_URL + "/invitations")
                        .header("Authorization", bearer(john))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new.leader@test.com\",\"fullName\":\"New Leader\",\"churchId\":%d}"
                                .formatted(grace.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("LEADER"))
                .andExpect(jsonPath("$.churchName").value("Victory Church"))
                .andReturn();

        String token = tokenOf(result);

        mockMvc.perform(post("/api/v1/invitations/accept").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"fullName\":\"New Leader\",\"password\":\"%s\"}"
                                .formatted(token, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("LEADER"));

        Long newId = users.findByEmail("new.leader@test.com").orElseThrow().getId();
        assertThat(leaders.findById(newId)).isPresent();
        assertThat(pastorLeaders.existsByLeaderIdAndPastorId(newId, john.getId())).isTrue();

        mockMvc.perform(get(PASTOR_URL + "/leaders").header("Authorization", bearer(john)))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get(PASTOR_URL + "/leaders").header("Authorization", bearer(mary)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void invitingAnExistingUserIsRejected() throws Exception {
        mockMvc.perform(post(PASTOR_URL + "/invitations").header("Authorization", bearer(john))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"amy@test.com\",\"fullName\":\"Amy\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void aPastorWithoutAChurchCannotInvite() throws Exception {
        User orphan = users.save(new User("orphan@test.com", "irrelevant", "Orphan Pastor", Role.PASTOR));

        mockMvc.perform(post(PASTOR_URL + "/invitations").header("Authorization", bearer(orphan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@test.com\",\"fullName\":\"X\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aPastorInvitationDoesNotCancelAnAdminInvitationForTheSameEmail() throws Exception {
        User admin = users.save(new User("admin@test.com", "irrelevant", "Admin", Role.ADMIN));
        MvcResult adminInvite = mockMvc.perform(post("/api/v1/admin/invitations")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"shared@test.com\",\"fullName\":\"Shared\",\"churchId\":%d}"
                                .formatted(victory.getId())))
                .andExpect(status().isCreated()).andReturn();
        String adminToken = tokenOf(adminInvite);

        mockMvc.perform(post(PASTOR_URL + "/invitations").header("Authorization", bearer(john))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"shared@test.com\",\"fullName\":\"Shared\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/invitations/preview").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\"}".formatted(adminToken)))
                .andExpect(status().isOk());
    }

    // ---------- ownership ----------

    @Test
    void aPastorSeesOnlyTheirOwnLeaders() throws Exception {
        mockMvc.perform(get(PASTOR_URL + "/leaders").header("Authorization", bearer(john)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Leader Amy"))
                .andExpect(jsonPath("$.content[0].pastorName").value("Pastor John"))
                .andExpect(jsonPath("$.content[0].churchName").value("Victory Church"));

        mockMvc.perform(get(PASTOR_URL + "/leaders/" + leaderOfJohn.getId())
                        .header("Authorization", bearer(john)))
                .andExpect(status().isOk());
    }

    @Test
    void anotherPastorsLeaderLooksLikeItDoesNotExist() throws Exception {
        mockMvc.perform(get(PASTOR_URL + "/leaders/" + leaderOfMary.getId())
                        .header("Authorization", bearer(john)))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch(PASTOR_URL + "/leaders/" + leaderOfMary.getId() + "/status")
                        .header("Authorization", bearer(john))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isNotFound());

        // and the leader was really left untouched
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ben@test.com\",\"password\":\"%s\"}".formatted(PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void aPastorSeesAndRevokesOnlyTheirOwnInvitations() throws Exception {
        MvcResult johnsInvite = invite(john, "one@test.com");
        MvcResult marysInvite = invite(mary, "two@test.com");
        Long johnsId = ((Number) JsonPath.read(johnsInvite.getResponse().getContentAsString(), "$.id")).longValue();
        Long marysId = ((Number) JsonPath.read(marysInvite.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(get(PASTOR_URL + "/invitations").header("Authorization", bearer(john)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("one@test.com"));

        mockMvc.perform(delete(PASTOR_URL + "/invitations/" + marysId).header("Authorization", bearer(john)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(PASTOR_URL + "/invitations/" + johnsId).header("Authorization", bearer(john)))
                .andExpect(status().isNoContent());
    }

    // ---------- enabling and disabling ----------

    @Test
    void aPastorCanDisableAndEnableTheirLeader() throws Exception {
        String token = bearer(john);

        mockMvc.perform(patch(PASTOR_URL + "/leaders/" + leaderOfJohn.getId() + "/status")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
        login("amy@test.com").andExpect(status().isForbidden());

        mockMvc.perform(patch(PASTOR_URL + "/leaders/" + leaderOfJohn.getId() + "/status")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        login("amy@test.com").andExpect(status().isOk());
    }

    @Test
    void pendingIsNotAValidManualStatus() throws Exception {
        mockMvc.perform(patch(PASTOR_URL + "/leaders/" + leaderOfJohn.getId() + "/status")
                        .header("Authorization", bearer(john))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---------- roles ----------

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "LEADER", "MEMBER"})
    void onlyPastorsReachThePastorArea(Role role) throws Exception {
        String token = bearer(role);

        mockMvc.perform(get(PASTOR_URL + "/leaders").header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(PASTOR_URL + "/invitations").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@test.com\",\"fullName\":\"X\"}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"PASTOR", "LEADER", "MEMBER"})
    void onlyAdminsReachTheAdminLeaderList(Role role) throws Exception {
        mockMvc.perform(get(ADMIN_URL).header("Authorization", bearer(role)))
                .andExpect(status().isForbidden());
    }
//
//    @Test
//    void adminSeesEveryLeaderAndCanDisableOne() throws Exception {
//        String token = bearer(Role.ADMIN);
//
//        mockMvc.perform(get(ADMIN_URL).header("Authorization", token))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.totalElements").value(2));
//        mockMvc.perform(get(ADMIN_URL).param("q", "grace").header("Authorization", token))
//                .andExpect(jsonPath("$.totalElements").value(1))
//                .andExpect(jsonPath("$.content[0].fullName").value("Leader Ben"));
//
//        mockMvc.perform(patch(ADMIN_URL + "/" + leaderOfMary.getId() + "/status")
//                        .header("Authorization", token)
//                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value("DISABLED"));
//        mockMvc.perform(patch(ADMIN_URL + "/999999/status").header("Authorization", token)
//                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
//                .andExpect(status().isNotFound());
//    }

    // ---------- helpers ----------

    private User pastor(String email, String fullName, Church church) {
        User user = users.save(new User(email, encoder.encode(PASSWORD), fullName, Role.PASTOR));
        pastors.save(new Pastor(user.getId(), church));
        return user;
    }

    private User leader(String email, String fullName, Church church, User pastor) {
        User user = users.save(new User(email, encoder.encode(PASSWORD), fullName, Role.LEADER));
        leaders.save(new CellGroupLeader(user.getId(), church));
        pastorLeaders.save(new PastorLeader(user.getId(), pastor.getId()));
        return user;
    }

    private MvcResult invite(User pastor, String email) throws Exception {
        return mockMvc.perform(post(PASTOR_URL + "/invitations").header("Authorization", bearer(pastor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"fullName\":\"Invitee\"}".formatted(email)))
                .andExpect(status().isCreated()).andReturn();
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)));
    }

    private static String tokenOf(MvcResult result) throws Exception {
        String url = JsonPath.read(result.getResponse().getContentAsString(), "$.inviteUrl");
        return url.substring(url.indexOf("token=") + "token=".length());
    }

    private String bearer(User user) {
        return "Bearer " + tokenService.createAccessToken(user).value();
    }

    private String bearer(Role role) {
        String email = "role-" + role.name().toLowerCase() + "@test.com";
        User user = users.findByEmail(email)
                .orElseGet(() -> users.save(new User(email, "irrelevant", "Test " + role, role)));
        return bearer(user);
    }
}