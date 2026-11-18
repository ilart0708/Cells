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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberRoomApiTest extends IntegrationTestBase {

    private static final String URL = "/api/v1/member/rooms";
    private static final String PASSWORD = "Passw0rd#12345";

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
    private User amy;
    private User ben;
    private Room youth;
    private Room adults;
    private Room benRoom;
    private User alice;
    private User bob;

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
        alice = member("alice@test.com", "Alice", victory);
        bob = member("bob@test.com", "Bob", victory);
    }

    // ---------- my rooms ----------

    @Test
    void aMemberSeesTheirRoomsWithTheirOwnNumber() throws Exception {
        enroll(youth, bob);        // Bob is #1
        enroll(youth, alice);      // Alice is #2
        enroll(adults, alice);     // Alice is #1 here

        mockMvc.perform(get(URL).header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].roomName").value("Young Adults"))
                .andExpect(jsonPath("$[0].myParticipationNumber").value(1))
                .andExpect(jsonPath("$[0].memberCount").value(1))
                .andExpect(jsonPath("$[1].roomName").value("Youth"))
                .andExpect(jsonPath("$[1].myParticipationNumber").value(2))
                .andExpect(jsonPath("$[1].memberCount").value(2))
                .andExpect(jsonPath("$[1].churchName").value("Victory Church"))
                .andExpect(jsonPath("$[1].leaderName").value("Leader Amy"));
    }

    @Test
    void aMemberWithoutRoomsGetsAnEmptyList() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void archivedRoomsAndRemovedMembershipsAreHidden() throws Exception {
        enroll(youth, alice);
        enroll(adults, alice);
        youth.archive(Instant.now());
        rooms.save(youth);
        roomMemberService.remove(amy.getId(), adults.getId(), alice.getId());

        mockMvc.perform(get(URL).header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- one room ----------

    @Test
    void theDetailShowsTheOrderWithNamesAndNumbersOnly() throws Exception {
        enroll(youth, bob);
        enroll(youth, alice);

        mockMvc.perform(get(URL + "/" + youth.getId()).header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.room.roomName").value("Youth"))
                .andExpect(jsonPath("$.room.myParticipationNumber").value(2))
                .andExpect(jsonPath("$.order.length()").value(2))
                .andExpect(jsonPath("$.order[0].participationNumber").value(1))
                .andExpect(jsonPath("$.order[0].fullName").value("Bob"))
                .andExpect(jsonPath("$.order[0].you").value(false))
                .andExpect(jsonPath("$.order[1].fullName").value("Alice"))
                .andExpect(jsonPath("$.order[1].you").value(true))
                .andExpect(jsonPath("$.order[0].email").doesNotExist())
                .andExpect(jsonPath("$.order[0].memberId").doesNotExist());
    }

    @Test
    void everyMemberSeesTheSameOrderWithTheirOwnMarker() throws Exception {
        enroll(youth, alice);
        enroll(youth, bob);

        mockMvc.perform(get(URL + "/" + youth.getId()).header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$.order[0].fullName").value("Alice"))
                .andExpect(jsonPath("$.order[0].you").value(false))
                .andExpect(jsonPath("$.order[1].fullName").value("Bob"))
                .andExpect(jsonPath("$.order[1].you").value(true));
    }

    @Test
    void theDetailFollowsReorderingAndRemoval() throws Exception {
        enroll(youth, alice);
        enroll(youth, bob);
        roomMemberService.reorder(amy.getId(), youth.getId(), java.util.List.of(bob.getId(), alice.getId()));

        mockMvc.perform(get(URL + "/" + youth.getId()).header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$.order[0].fullName").value("Bob"))
                .andExpect(jsonPath("$.room.myParticipationNumber").value(2));

        roomMemberService.remove(amy.getId(), youth.getId(), bob.getId());

        mockMvc.perform(get(URL + "/" + youth.getId()).header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$.order.length()").value(1))
                .andExpect(jsonPath("$.room.myParticipationNumber").value(1));
    }

    @Test
    void aRoomTheMemberIsNotInLooksLikeItDoesNotExist() throws Exception {
        enroll(youth, bob);
        enroll(benRoom, member("zed@test.com", "Zed", grace));

        mockMvc.perform(get(URL + "/" + youth.getId()).header("Authorization", bearer(alice)))
                .andExpect(status().isNotFound());                                   // same church, not in it
        mockMvc.perform(get(URL + "/" + benRoom.getId()).header("Authorization", bearer(alice)))
                .andExpect(status().isNotFound());                                   // other church
        mockMvc.perform(get(URL + "/999999").header("Authorization", bearer(alice)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(URL + "/abc").header("Authorization", bearer(alice)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aRemovedMemberAndAnArchivedRoomAreNotReachableById() throws Exception {
        enroll(youth, alice);
        enroll(adults, alice);
        roomMemberService.remove(amy.getId(), youth.getId(), alice.getId());
        adults.archive(Instant.now());
        rooms.save(adults);

        mockMvc.perform(get(URL + "/" + youth.getId()).header("Authorization", bearer(alice)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(URL + "/" + adults.getId()).header("Authorization", bearer(alice)))
                .andExpect(status().isNotFound());
    }

    // ---------- roles ----------

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "PASTOR", "LEADER"})
    void onlyMembersCanUseTheseEndpoints(Role role) throws Exception {
        String token = bearer(role);

        mockMvc.perform(get(URL).header("Authorization", token)).andExpect(status().isForbidden());
        mockMvc.perform(get(URL + "/" + youth.getId()).header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void noTokenReturns401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
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

    private void enroll(Room room, User person) {
        roomMemberService.enroll(room.getLeaderId(), room.getId(), person.getId());
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