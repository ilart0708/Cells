package com.cells.backend.room;

import com.cells.backend.IntegrationTestBase;
import com.cells.backend.auth.TokenService;
import com.cells.backend.church.Church;
import com.cells.backend.church.ChurchRepository;
import com.cells.backend.common.BusinessRuleException;
import com.cells.backend.common.DuplicateResourceException;
import com.cells.backend.common.ResourceNotFoundException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomMemberApiTest extends IntegrationTestBase {

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

    // ---------- roster ----------

    @Test
    void theRosterListsActiveMembersInParticipationOrder() throws Exception {
        enroll(youth, member("alice@test.com", "Alice", victory));
        enroll(youth, member("bob@test.com", "Bob", victory));
        enroll(youth, member("carol@test.com", "Carol", victory));

        mockMvc.perform(get(roster(youth)).header("Authorization", bearer(amy)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].fullName").value("Alice"))
                .andExpect(jsonPath("$[0].participationNumber").value(1))
                .andExpect(jsonPath("$[0].email").value("alice@test.com"))
                .andExpect(jsonPath("$[0].accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$[1].fullName").value("Bob"))
                .andExpect(jsonPath("$[1].participationNumber").value(2))
                .andExpect(jsonPath("$[2].fullName").value("Carol"))
                .andExpect(jsonPath("$[2].participationNumber").value(3))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void anEmptyRoomHasAnEmptyRoster() throws Exception {
        mockMvc.perform(get(roster(youth)).header("Authorization", bearer(amy)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void removedMembersAreLeftOutOfTheRoster() throws Exception {
        User alice = member("alice@test.com", "Alice", victory);
        enroll(youth, alice);
        enroll(youth, member("bob@test.com", "Bob", victory));
        remove(youth, alice);

        mockMvc.perform(get(roster(youth)).header("Authorization", bearer(amy)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fullName").value("Bob"));
    }

    @Test
    void theRoomListShowsTheActiveMemberCount() throws Exception {
        User alice = member("alice@test.com", "Alice", victory);
        enroll(youth, alice);
        enroll(youth, member("bob@test.com", "Bob", victory));
        remove(youth, alice);

        mockMvc.perform(get("/api/v1/leader/rooms").header("Authorization", bearer(amy)))
                .andExpect(jsonPath("$.content[?(@.name=='Youth')].memberCount").value(1))
                .andExpect(jsonPath("$.content[?(@.name=='Young Adults')].memberCount").value(0));
    }

    // ---------- numbering ----------

    @Test
    void numbersStartAtOneAndGrowByOne() {
        assertThat(enroll(youth, member("alice@test.com", "Alice", victory)).participationNumber()).isEqualTo(1);
        assertThat(enroll(youth, member("bob@test.com", "Bob", victory)).participationNumber()).isEqualTo(2);
        assertThat(enroll(youth, member("carol@test.com", "Carol", victory)).participationNumber()).isEqualTo(3);
    }

    @Test
    void theSameMemberHasAnIndependentNumberInEachRoom() {
        User alice = member("alice@test.com", "Alice", victory);
        User bob = member("bob@test.com", "Bob", victory);

        enroll(youth, alice);
        assertThat(enroll(youth, bob).participationNumber()).isEqualTo(2);
        assertThat(enroll(adults, bob).participationNumber()).isEqualTo(1);
    }

    @Test
    void concurrentEnrollmentsGetDistinctConsecutiveNumbers() throws Exception {
        List<User> people = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> member("member" + i + "@test.com", "Member " + i, victory))
                .toList();

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        for (User person : people) {
            futures.add(pool.submit(() -> {
                start.await();
                return enroll(youth, person).participationNumber();
            }));
        }
        start.countDown();

        List<Integer> numbers = new ArrayList<>();
        for (Future<Integer> future : futures) {
            numbers.add(future.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(numbers).containsExactlyInAnyOrderElementsOf(IntStream.rangeClosed(1, 10).boxed().toList());
    }

    // ---------- enrollment rules ----------

    @Test
    void aMemberCannotJoinTheSameRoomTwice() {
        User alice = member("alice@test.com", "Alice", victory);
        enroll(youth, alice);

        assertThatThrownBy(() -> enroll(youth, alice)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void anArchivedRoomRefusesNewMembers() {
        youth.archive(Instant.now());
        rooms.save(youth);

        assertThatThrownBy(() -> enroll(youth, member("alice@test.com", "Alice", victory)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Archived");
    }

    @Test
    void aMemberOfAnotherChurchLooksLikeItDoesNotExist() {
        User outsider = member("zed@test.com", "Zed", grace);

        assertThatThrownBy(() -> enroll(youth, outsider)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void aDisabledAccountIsRejected() {
        User alice = member("alice@test.com", "Alice", victory);
        alice.disable();
        users.save(alice);

        assertThatThrownBy(() -> enroll(youth, alice)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void aUserWhoIsNotAMemberIsRejected() {
        assertThatThrownBy(() -> roomMemberService.enroll(amy.getId(), youth.getId(), ben.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void onlyTheRoomsOwnerCanEnroll() {
        User alice = member("alice@test.com", "Alice", victory);

        assertThatThrownBy(() -> roomMemberService.enroll(ben.getId(), youth.getId(), alice.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(roomMembers.maxActiveNumber(youth.getId())).isZero();
    }

    // ---------- the database guards the rules too ----------

    @Test
    void theDatabaseRejectsTwoActiveMembersWithTheSameNumber() {
        User alice = member("alice@test.com", "Alice", victory);
        User bob = member("bob@test.com", "Bob", victory);
        roomMembers.saveAndFlush(new RoomMember(youth.getId(), alice.getId(), 1));

        assertThatThrownBy(() -> roomMembers.saveAndFlush(new RoomMember(youth.getId(), bob.getId(), 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theDatabaseRejectsTheSameMemberTwiceInOneRoom() {
        User alice = member("alice@test.com", "Alice", victory);
        roomMembers.saveAndFlush(new RoomMember(youth.getId(), alice.getId(), 1));

        assertThatThrownBy(() -> roomMembers.saveAndFlush(new RoomMember(youth.getId(), alice.getId(), 2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aRemovedMembershipNoLongerBlocksItsNumberOrItsMember() {
        User alice = member("alice@test.com", "Alice", victory);
        User bob = member("bob@test.com", "Bob", victory);
        RoomMember first = roomMembers.saveAndFlush(new RoomMember(youth.getId(), alice.getId(), 1));

        first.markRemoved(Instant.now());
        roomMembers.saveAndFlush(first);

        roomMembers.saveAndFlush(new RoomMember(youth.getId(), bob.getId(), 1));     // number is free again
        roomMembers.saveAndFlush(new RoomMember(youth.getId(), alice.getId(), 2));   // Alice may rejoin
        assertThat(roomMembers.findAll()).hasSize(3);                                 // history is kept
    }

    // ---------- who may see a roster ----------

    @Test
    void anotherLeadersRoomRosterLooksLikeItDoesNotExist() throws Exception {
        enroll(benRoom, member("zed@test.com", "Zed", grace));

        mockMvc.perform(get(roster(benRoom)).header("Authorization", bearer(amy)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(roster(benRoom)).header("Authorization", bearer(ben)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void unknownAndMalformedRoomIdsAreHandled() throws Exception {
        mockMvc.perform(get("/api/v1/leader/rooms/999999/members").header("Authorization", bearer(amy)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/leader/rooms/abc/members").header("Authorization", bearer(amy)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "PASTOR", "MEMBER"})
    void onlyLeadersCanSeeARoster(Role role) throws Exception {
        mockMvc.perform(get(roster(youth)).header("Authorization", bearer(role)))
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

    private User member(String email, String fullName, Church church) {
        User user = users.save(new User(email, encoder.encode(PASSWORD), fullName, Role.MEMBER));
        members.save(new Member(user.getId(), church));
        return user;
    }

    private RoomMemberResponse enroll(Room room, User member) {
        return roomMemberService.enroll(room.getLeaderId(), room.getId(), member.getId());
    }

    private void remove(Room room, User member) {
        RoomMember membership = roomMembers
                .findByRoomIdAndMemberIdAndRemovedAtIsNull(room.getId(), member.getId()).orElseThrow();
        membership.markRemoved(Instant.now());
        roomMembers.save(membership);
    }

    private static String roster(Room room) {
        return "/api/v1/leader/rooms/" + room.getId() + "/members";
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