package com.cells.backend.room;

import com.cells.backend.IntegrationTestBase;
import com.cells.backend.auth.TokenService;
import com.cells.backend.church.Church;
import com.cells.backend.church.ChurchRepository;
import com.cells.backend.leader.CellGroupLeader;
import com.cells.backend.leader.CellGroupLeaderRepository;
import com.cells.backend.leader.PastorLeader;
import com.cells.backend.leader.PastorLeaderRepository;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomApiTest extends IntegrationTestBase {

    private static final String URL = "/api/v1/leader/rooms";
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
    private User amy;   // leader at Victory
    private User ben;   // leader at Grace

    @BeforeEach
    void setUp() {
        victory = churches.save(new Church("Victory Church"));
        grace = churches.save(new Church("Grace Community Church"));
        User john = pastor("john@test.com", "Pastor John", victory);
        User mary = pastor("mary@test.com", "Pastor Mary", grace);
        amy = leader("amy@test.com", "Leader Amy", victory, john);
        ben = leader("ben@test.com", "Leader Ben", grace, mary);
    }

    // ---------- creating ----------

    @Test
    void aLeaderCreatesARoomInTheirOwnChurchEvenIfTheyAskForAnotherOne() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(URL).header("Authorization", bearer(amy))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Youth Cell Group\",\"churchId\":%d}".formatted(grace.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Youth Cell Group"))
                .andExpect(jsonPath("$.churchName").value("Victory Church"))
                .andExpect(jsonPath("$.leaderName").value("Leader Amy"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void namesAreTrimmedAndDuplicatesAreRejectedIgnoringCase() throws Exception {
        createRoom(amy, "Youth Cell Group");

        post(amy, URL, body("  youth cell group ")).andExpect(status().isConflict());
    }

    @Test
    void differentLeadersMayUseTheSameName() throws Exception {
        createRoom(amy, "Youth");
        createRoom(ben, "Youth");
    }

    @Test
    void blankAndTooLongNamesAreRejected() throws Exception {
        post(amy, URL, body("   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
        post(amy, URL, body("x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void aLeaderUserWithoutALeaderRecordCannotCreateRooms() throws Exception {
        User orphan = users.save(new User("orphan@test.com", "irrelevant", "Orphan Leader", Role.LEADER));

        post(orphan, URL, body("Youth")).andExpect(status().isUnprocessableEntity());
    }

    // ---------- listing ----------

    @Test
    void aLeaderSeesOnlyTheirOwnRoomsSortedByName() throws Exception {
        createRoom(amy, "Zeta Group");
        createRoom(amy, "alpha group");
        createRoom(ben, "Ben's Room");

        mockMvc.perform(get(URL).header("Authorization", bearer(amy)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("alpha group"))
                .andExpect(jsonPath("$.content[1].name").value("Zeta Group"));
    }

    @Test
    void searchAndStatusFilterWork() throws Exception {
        createRoom(amy, "Youth Cell Group");
        long adults = createRoom(amy, "Young Adults");
        setStatus(amy, adults, "ARCHIVED").andExpect(status().isOk());
        String token = bearer(amy);

        mockMvc.perform(get(URL).header("Authorization", token))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Youth Cell Group"));
        mockMvc.perform(get(URL).param("status", "ARCHIVED").header("Authorization", token))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Young Adults"));
        mockMvc.perform(get(URL).param("status", "ALL").header("Authorization", token))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get(URL).param("status", "ALL").param("q", "YOUNG").header("Authorization", token))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void badParameterValuesAnswer400() throws Exception {
        mockMvc.perform(get(URL).param("status", "BANANA").header("Authorization", bearer(amy)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL + "/abc").header("Authorization", bearer(amy)))
                .andExpect(status().isBadRequest());
    }

    // ---------- ownership ----------

    @Test
    void anotherLeadersRoomLooksLikeItDoesNotExist() throws Exception {
        long benRoom = createRoom(ben, "Ben's Room");

        mockMvc.perform(get(URL + "/" + benRoom).header("Authorization", bearer(amy)))
                .andExpect(status().isNotFound());
        put(amy, URL + "/" + benRoom, body("Hijacked")).andExpect(status().isNotFound());
        setStatus(amy, benRoom, "ARCHIVED").andExpect(status().isNotFound());

        // and the room really was left untouched
        mockMvc.perform(get(URL + "/" + benRoom).header("Authorization", bearer(ben)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ben's Room"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ---------- renaming ----------

    @Test
    void renameWorksAndMayChangeOnlyTheCaseOfItsOwnName() throws Exception {
        long id = createRoom(amy, "Youth");

        put(amy, URL + "/" + id, body("Youth Group"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Youth Group"));
        put(amy, URL + "/" + id, body("YOUTH GROUP")).andExpect(status().isOk());
    }

    @Test
    void renamingToAnotherOfMyRoomsNamesIsRejected() throws Exception {
        createRoom(amy, "Young Adults");
        long youth = createRoom(amy, "Youth");

        put(amy, URL + "/" + youth, body("young adults")).andExpect(status().isConflict());
    }

    // ---------- archiving and restoring ----------

    @Test
    void archivingKeepsTheRoomAndFreesItsName() throws Exception {
        long old = createRoom(amy, "Youth");

        setStatus(amy, old, "ARCHIVED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.archivedAt").isNotEmpty());
        setStatus(amy, old, "ARCHIVED").andExpect(status().isOk()); // archiving twice is harmless

        mockMvc.perform(get(URL + "/" + old).header("Authorization", bearer(amy)))
                .andExpect(status().isOk());      // still reachable by id: history is kept
        createRoom(amy, "Youth");                  // the name is free again
    }

    @Test
    void anArchivedRoomMustBeRestoredBeforeItIsRenamed() throws Exception {
        long id = createRoom(amy, "Youth");
        setStatus(amy, id, "ARCHIVED").andExpect(status().isOk());

        put(amy, URL + "/" + id, body("Youth Group")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void restoringFailsWhileAnotherActiveRoomHasTheName() throws Exception {
        long old = createRoom(amy, "Youth");
        setStatus(amy, old, "ARCHIVED").andExpect(status().isOk());
        long replacement = createRoom(amy, "Youth");

        setStatus(amy, old, "ACTIVE").andExpect(status().isConflict());

        setStatus(amy, replacement, "ARCHIVED").andExpect(status().isOk());
        setStatus(amy, old, "ACTIVE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.archivedAt").doesNotExist());
    }

    @Test
    void anUnknownStatusIsRejected() throws Exception {
        long id = createRoom(amy, "Youth");

        mockMvc.perform(patch(URL + "/" + id + "/status").header("Authorization", bearer(amy))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"BANANA\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- roles ----------

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "PASTOR", "MEMBER"})
    void onlyLeadersReachTheRoomEndpoints(Role role) throws Exception {
        String token = bearer(role);

        mockMvc.perform(get(URL).header("Authorization", token)).andExpect(status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.post(URL).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(body("Youth")))
                .andExpect(status().isForbidden());
    }

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

    private long createRoom(User leader, String name) throws Exception {
        MvcResult result = post(leader, URL, body(name)).andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private ResultActions post(User user, String url, String json) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url).header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions put(User user, String url, String json) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.put(url).header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions setStatus(User user, long roomId, String status) throws Exception {
        return mockMvc.perform(patch(URL + "/" + roomId + "/status").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"%s\"}".formatted(status)));
    }

    private static String body(String name) {
        return "{\"name\":\"%s\"}".formatted(name);
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