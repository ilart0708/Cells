package com.cells.backend.room;

import com.cells.backend.IntegrationTestBase;
import com.cells.backend.auth.TokenService;
import com.cells.backend.church.Church;
import com.cells.backend.church.ChurchRepository;
import com.cells.backend.leader.CellGroupLeader;
import com.cells.backend.leader.CellGroupLeaderRepository;
import com.cells.backend.leader.PastorLeader;
import com.cells.backend.leader.PastorLeaderRepository;
import com.cells.backend.member.Member;
import com.cells.backend.member.MemberRepository;
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

import java.time.Instant;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberEnrollmentApiTest extends IntegrationTestBase {

    private static final String PASSWORD = "Passw0rd#12345";
    private static final String SAME_CHURCH_MESSAGE =
            "This person already has an account. Add them from the list of church members.";
    private static final String CANNOT_INVITE_MESSAGE = "This email address cannot be invited to the room.";

    @Autowired private UserRepository users;
    @Autowired private ChurchRepository churches;
    @Autowired private PastorRepository pastors;
    @Autowired private CellGroupLeaderRepository leaders;
    @Autowired private PastorLeaderRepository pastorLeaders;
    @Autowired private MemberRepository members;
    @Autowired private RoomRepository rooms;
    @Autowired private RoomMemberRepository roomMembers;
    @Autowired private RoomMemberService roomMemberService;
    @Autowired private TokenService tokenService;
    @Autowired private PasswordEncoder encoder;

    private Church victory;
    private Church grace;
    private User amy;       // leader at Victory
    private User ben;       // leader at Grace
    private Room youth;     // Amy's room
    private Room adults;    // Amy's second room
    private Room benRoom;   // Ben's room

    @BeforeEach
    void setUp() {
        victory = churches.save(new Church("Victory Church"));
        grace = churches.save(new Church("Grace Community Church"));
        User john = pastor("john@test.com", "Pastor John", victory);
        User mary = pastor("mary@test.com", "Pastor Mary", grace);
        amy = leader("amy@test.com", "Leader Amy", victory, john);
        ben = leader("ben@test.com", "Leader Ben", grace, mary);
        youth = rooms.save(new Room(victory, amy.getId(), "Youth"));
        adults = rooms.save(new Room(victory, amy.getId(), "Young Adults"));
        benRoom = rooms.save(new Room(grace, ben.getId(), "Ben's Room"));
    }

    // ---------- people I can add ----------

    @Test
    void availableMembersAreActiveChurchMembersNotYetInTheRoom() throws Exception {
        User alice = member("alice@test.com", "Alice", victory);
        member("bob@test.com", "Bob", victory);
        member("carol@test.com", "Carol", victory);
        User dan = member("dan@test.com", "Dan", victory);
        dan.disable();
        users.save(dan);
        member("zed@test.com", "Zed", grace);
        enroll(youth, alice);

        mockMvc.perform(get(available(youth)).header("Authorization", bearer(amy)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].fullName").value("Bob"))
                .andExpect(jsonPath("$.content[1].fullName").value("Carol"))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    @Test
    void searchNarrowsTheListAndAMemberOfAnotherRoomStaysAvailable() throws Exception {
        User alice = member("alice@test.com", "Alice", victory);
        member("carol@test.com", "Carol", victory);
        enroll(youth, alice);

        mockMvc.perform(get(available(youth)).param("q", "CAR").header("Authorization", bearer(amy)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Carol"));
        mockMvc.perform(get(available(adults)).header("Authorization", bearer(amy)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void aRemovedMemberIsAvailableAgain() throws Exception {
        User alice = member("alice@test.com", "Alice", victory);
        enroll(youth, alice);
        RoomMember membership = roomMembers
                .findByRoomIdAndMemberIdAndRemovedAtIsNull(youth.getId(), alice.getId()).orElseThrow();
        membership.markRemoved(Instant.now());
        roomMembers.save(membership);

        mockMvc.perform(get(available(youth)).header("Authorization", bearer(amy)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void theAvailableListOfAnotherLeadersRoomIs404() throws Exception {
        mockMvc.perform(get(available(benRoom)).header("Authorization", bearer(amy)))
                .andExpect(status().isNotFound());
    }

    // ---------- adding an existing member ----------

    @Test
    void anExistingMemberIsAddedWithTheNextNumber() throws Exception {
        User alice = member("alice@test.com", "Alice", victory);
        User bob = member("bob@test.com", "Bob", victory);

        addExisting(amy, youth, alice.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Alice"))
                .andExpect(jsonPath("$.participationNumber").value(1));
        addExisting(amy, youth, bob.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.participationNumber").value(2));

        mockMvc.perform(get(members(youth)).header("Authorization", bearer(amy)))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].fullName").value("Bob"));
    }

    @Test
    void theSameMemberCannotBeAddedTwice() throws Exception {
        User alice = member("alice@test.com", "Alice", victory);
        addExisting(amy, youth, alice.getId()).andExpect(status().isCreated());

        addExisting(amy, youth, alice.getId()).andExpect(status().isConflict());
    }

    @Test
    void aMemberOfAnotherChurchLooksUnknown() throws Exception {
        User zed = member("zed@test.com", "Zed", grace);

        addExisting(amy, youth, zed.getId()).andExpect(status().isNotFound());
        assertThat(roomMembers.maxActiveNumber(youth.getId())).isZero();
    }

    @Test
    void unknownAndMissingMemberIdsAreRejected() throws Exception {
        addExisting(amy, youth, 999999L).andExpect(status().isNotFound());

        mockMvc.perform(post(members(youth)).header("Authorization", bearer(amy))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.memberId").exists());
    }

    @Test
    void aDisabledMemberCannotBeAdded() throws Exception {
        User alice = member("alice@test.com", "Alice", victory);
        alice.disable();
        users.save(alice);

        addExisting(amy, youth, alice.getId()).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void anotherLeadersRoomCannotBeChanged() throws Exception {
        User alice = member("alice@test.com", "Alice", victory);

        addExisting(amy, benRoom, alice.getId()).andExpect(status().isNotFound());
        invite(amy, benRoom, "new@test.com", "New Person").andExpect(status().isNotFound());
        assertThat(roomMembers.maxActiveNumber(benRoom.getId())).isZero();
    }

    @Test
    void anArchivedRoomRefusesBothWays() throws Exception {
        User alice = member("alice@test.com", "Alice", victory);
        youth.archive(Instant.now());
        rooms.save(youth);

        addExisting(amy, youth, alice.getId()).andExpect(status().isUnprocessableEntity());
        invite(amy, youth, "new@test.com", "New Person").andExpect(status().isUnprocessableEntity());
    }

    // ---------- inviting a new person ----------

    @Test
    void aLeaderInvitesANewPersonIntoTheirRoom() throws Exception {
        mockMvc.perform(post(invitations(youth)).header("Authorization", bearer(amy))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"New@Test.com\",\"fullName\":\"New Person\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.email").value("new@test.com"))
                .andExpect(jsonPath("$.churchName").value("Victory Church"))
                .andExpect(jsonPath("$.roomName").value("Youth"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.inviteUrl").isNotEmpty());
    }

    @Test
    void existingAccountsAreNeverInvitedAndOnlyOwnChurchMembersAreDescribed() throws Exception {
        member("alice@test.com", "Alice", victory);
        member("zed@test.com", "Zed", grace);

        invite(amy, youth, "alice@test.com", "Alice")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(SAME_CHURCH_MESSAGE));
        invite(amy, youth, "zed@test.com", "Zed")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(CANNOT_INVITE_MESSAGE));
        invite(amy, youth, "john@test.com", "Pastor John")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(CANNOT_INVITE_MESSAGE));
    }

    @Test
    void aNewInvitationForTheSameRoomReplacesTheOldOne() throws Exception {
        String first = tokenOf(invite(amy, youth, "new@test.com", "New Person").andReturn());
        String second = tokenOf(invite(amy, youth, "new@test.com", "New Person").andReturn());

        preview(first).andExpect(status().isGone());
        preview(second).andExpect(status().isOk());
    }

    @Test
    void invitationsIntoDifferentRoomsDoNotReplaceEachOther() throws Exception {
        String forYouth = tokenOf(invite(amy, youth, "new@test.com", "New Person").andReturn());
        String forAdults = tokenOf(invite(amy, adults, "new@test.com", "New Person").andReturn());

        preview(forYouth).andExpect(status().isOk()).andExpect(jsonPath("$.roomName").value("Youth"));
        preview(forAdults).andExpect(status().isOk()).andExpect(jsonPath("$.roomName").value("Young Adults"));
    }

    @Test
    void anInvalidInvitationBodyReturnsFieldErrors() throws Exception {
        invite(amy, youth, "not-an-email", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.fullName").exists());
    }

    // ---------- accepting ----------

    @Test
    void anInvitedPersonJoinsTheRoomWithTheNextNumber() throws Exception {
        enroll(youth, member("alice@test.com", "Alice", victory));
        String token = tokenOf(invite(amy, youth, "new@test.com", "New Person").andReturn());

        preview(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.churchName").value("Victory Church"))
                .andExpect(jsonPath("$.roomName").value("Youth"))
                .andExpect(jsonPath("$.leaderName").value("Leader Amy"));

        MvcResult accepted = accept(token, "New Person")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("MEMBER"))
                .andReturn();
        assertThat(accepted.getResponse().getHeader("Set-Cookie")).startsWith("cells_refresh=");

        User created = users.findByEmail("new@test.com").orElseThrow();
        assertThat(members.existsById(created.getId())).isTrue();
        assertThat(members.findById(created.getId()).orElseThrow().getChurch().getId()).isEqualTo(victory.getId());

        mockMvc.perform(get(members(youth)).header("Authorization", bearer(amy)))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].fullName").value("New Person"))
                .andExpect(jsonPath("$[1].participationNumber").value(2));

        mockMvc.perform(get(invitations(youth)).header("Authorization", bearer(amy)))
                .andExpect(jsonPath("$.content[0].status").value("ACCEPTED"));

        accept(token, "New Person").andExpect(status().isGone());   // single use
    }

    @Test
    void anInvitationIntoAnArchivedRoomCannotBeUsed() throws Exception {
        String token = tokenOf(invite(amy, youth, "new@test.com", "New Person").andReturn());
        youth.archive(Instant.now());
        rooms.save(youth);

        preview(token).andExpect(status().isGone());
        accept(token, "New Person").andExpect(status().isGone());
        assertThat(users.existsByEmail("new@test.com")).isFalse();
    }

    @Test
    void twoInvitedPeopleAcceptingAtOnceGetDistinctNumbers() throws Exception {
        String first = tokenOf(invite(amy, youth, "one@test.com", "One").andReturn());
        String second = tokenOf(invite(amy, youth, "two@test.com", "Two").andReturn());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = List.of(
                pool.submit(() -> {
                    start.await();
                    return accept(first, "One").andReturn().getResponse().getStatus();
                }),
                pool.submit(() -> {
                    start.await();
                    return accept(second, "Two").andReturn().getResponse().getStatus();
                }));
        start.countDown();
        for (Future<Integer> future : futures) {
            assertThat(future.get(30, TimeUnit.SECONDS)).isEqualTo(200);
        }
        pool.shutdown();

        List<Integer> numbers = roomMembers.roster(youth.getId()).stream()
                .map(RoomMemberResponse::participationNumber).toList();
        assertThat(numbers).containsExactly(1, 2);
    }

    // ---------- seeing and revoking a room's invitations ----------

    @Test
    void aLeaderSeesAndRevokesOnlyTheirRoomsInvitations() throws Exception {
        MvcResult mine = invite(amy, youth, "one@test.com", "One").andReturn();
        MvcResult bens = invite(ben, benRoom, "two@test.com", "Two").andReturn();
        long mineId = idOf(mine);
        long bensId = idOf(bens);
        String mineToken = tokenOf(mine);

        mockMvc.perform(get(invitations(youth)).header("Authorization", bearer(amy)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("one@test.com"))
                .andExpect(jsonPath("$.content[0].inviteUrl").doesNotExist());
        mockMvc.perform(get(invitations(benRoom)).header("Authorization", bearer(amy)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(invitations(benRoom) + "/" + bensId).header("Authorization", bearer(amy)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(invitations(youth) + "/" + bensId).header("Authorization", bearer(amy)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(invitations(youth) + "/" + mineId).header("Authorization", bearer(amy)))
                .andExpect(status().isNoContent());
        preview(mineToken).andExpect(status().isGone());
    }

    // ---------- roles ----------

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "PASTOR", "MEMBER"})
    void onlyLeadersCanUseTheseEndpoints(Role role) throws Exception {
        String token = bearer(role);

        mockMvc.perform(get(available(youth)).header("Authorization", token)).andExpect(status().isForbidden());
        mockMvc.perform(post(members(youth)).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"memberId\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(invitations(youth)).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@test.com\",\"fullName\":\"X\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(invitations(youth)).header("Authorization", token)).andExpect(status().isForbidden());
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

    private User member(String email, String fullName, Church church) {
        User user = users.save(new User(email, encoder.encode(PASSWORD), fullName, Role.MEMBER));
        members.save(new Member(user.getId(), church));
        return user;
    }

    private void enroll(Room room, User member) {
        roomMemberService.enroll(room.getLeaderId(), room.getId(), member.getId());
    }

    private ResultActions addExisting(User leader, Room room, Long memberId) throws Exception {
        return mockMvc.perform(post(members(room)).header("Authorization", bearer(leader))
                .contentType(MediaType.APPLICATION_JSON).content("{\"memberId\":%d}".formatted(memberId)));
    }

    private ResultActions invite(User leader, Room room, String email, String fullName) throws Exception {
        return mockMvc.perform(post(invitations(room)).header("Authorization", bearer(leader))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"fullName\":\"%s\"}".formatted(email, fullName)));
    }

    private ResultActions preview(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/invitations/preview").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\"}".formatted(token)));
    }

    private ResultActions accept(String token, String fullName) throws Exception {
        return mockMvc.perform(post("/api/v1/invitations/accept").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"fullName\":\"%s\",\"password\":\"%s\"}"
                        .formatted(token, fullName, PASSWORD)));
    }

    private static String tokenOf(MvcResult result) throws Exception {
        String url = JsonPath.read(result.getResponse().getContentAsString(), "$.inviteUrl");
        return url.substring(url.indexOf("token=") + "token=".length());
    }

    private static long idOf(MvcResult result) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private static String members(Room room) {
        return "/api/v1/leader/rooms/" + room.getId() + "/members";
    }

    private static String available(Room room) {
        return members(room) + "/available";
    }

    private static String invitations(Room room) {
        return "/api/v1/leader/rooms/" + room.getId() + "/invitations";
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