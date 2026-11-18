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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ParticipationOrderApiTest extends IntegrationTestBase {

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
    private Room benRoom;   // Ben's room
    private User alice;
    private User bob;
    private User carol;

    @BeforeEach
    void setUp() {
        victory = churches.save(new Church("Victory Church"));
        grace = churches.save(new Church("Grace Community Church"));
        User john = pastor("john@test.com", "Pastor John", victory);
        User mary = pastor("mary@test.com", "Pastor Mary", grace);
        amy = leader("amy@test.com", "Leader Amy", victory, john);
        ben = leader("ben@test.com", "Leader Ben", grace, mary);
        youth = rooms.save(new Room(victory, amy.getId(), "Youth"));
        benRoom = rooms.save(new Room(grace, ben.getId(), "Ben's Room"));
        alice = member("alice@test.com", "Alice", victory);
        bob = member("bob@test.com", "Bob", victory);
        carol = member("carol@test.com", "Carol", victory);
    }

    // ---------- reordering ----------

    @Test
    void theLeaderReordersTheRoom() throws Exception {
        enroll(youth, alice, bob, carol);

        reorder(amy, youth, carol, alice, bob)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].fullName").value("Carol"))
                .andExpect(jsonPath("$[0].participationNumber").value(1))
                .andExpect(jsonPath("$[1].fullName").value("Alice"))
                .andExpect(jsonPath("$[1].participationNumber").value(2))
                .andExpect(jsonPath("$[2].fullName").value("Bob"))
                .andExpect(jsonPath("$[2].participationNumber").value(3));

        assertThat(names(youth)).containsExactly("Carol", "Alice", "Bob");
        assertContiguous(youth);
    }

    @Test
    void swappingTwoNeighboursWorksDespiteTheUniqueIndex() throws Exception {
        enroll(youth, alice, bob);

        reorder(amy, youth, bob, alice).andExpect(status().isOk());

        assertThat(names(youth)).containsExactly("Bob", "Alice");
        assertContiguous(youth);
    }

    @Test
    void reorderingToTheSameOrderChangesNothing() throws Exception {
        enroll(youth, alice, bob, carol);

        reorder(amy, youth, alice, bob, carol).andExpect(status().isOk());

        assertThat(names(youth)).containsExactly("Alice", "Bob", "Carol");
    }

    @Test
    void aReorderMustListEveryCurrentMemberExactlyOnce() throws Exception {
        enroll(youth, alice, bob, carol);
        User dan = member("dan@test.com", "Dan", victory);          // church member, not in this room
        User zed = member("zed@test.com", "Zed", grace);            // other church

        reorder(amy, youth, alice, bob).andExpect(status().isUnprocessableEntity());               // missing
        reorder(amy, youth, alice, bob, carol, dan).andExpect(status().isUnprocessableEntity());   // extra
        reorder(amy, youth, alice, bob, zed).andExpect(status().isUnprocessableEntity());          // unknown
        reorder(amy, youth, alice, alice, bob).andExpect(status().isUnprocessableEntity());        // duplicate
        reorderIds(amy, youth).andExpect(status().isUnprocessableEntity());                        // empty

        assertThat(names(youth)).containsExactly("Alice", "Bob", "Carol");                         // untouched
    }

    @Test
    void aMalformedBodyIsRejected() throws Exception {
        enroll(youth, alice);

        mockMvc.perform(put(members(youth) + "/order").header("Authorization", bearer(amy))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.memberIds").exists());
        mockMvc.perform(put(members(youth) + "/order").header("Authorization", bearer(amy))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"memberIds\":[null]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherLeadersRoomCannotBeReordered() throws Exception {
        enroll(benRoom, member("zed@test.com", "Zed", grace));

        reorder(amy, benRoom, alice).andExpect(status().isNotFound());
        assertThat(names(benRoom)).containsExactly("Zed");
    }

    @Test
    void aReorderRacingWithAnAddLeavesAConsistentRoom() throws Exception {
        List<User> five = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> member("m" + i + "@test.com", "M" + i, victory)).toList();
        enroll(youth, five.toArray(new User[0]));
        User late = member("late@test.com", "Late", victory);

        List<User> reversed = new ArrayList<>(five);
        java.util.Collections.reverse(reversed);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Integer> reorderStatus = pool.submit(() -> {
            start.await();
            return reorder(amy, youth, reversed.toArray(new User[0])).andReturn().getResponse().getStatus();
        });
        Future<Integer> addStatus = pool.submit(() -> {
            start.await();
            return addExisting(amy, youth, late).andReturn().getResponse().getStatus();
        });
        start.countDown();

        // Whoever got the room lock first wins: either the reorder ran first (200) or the add did and
        // the reorder was told its list is out of date (422). The add always succeeds.
        assertThat(reorderStatus.get(30, TimeUnit.SECONDS)).isIn(200, 422);
        assertThat(addStatus.get(30, TimeUnit.SECONDS)).isEqualTo(201);
        pool.shutdown();

        assertThat(roomMembers.roster(youth.getId())).hasSize(6);
        assertContiguous(youth);
    }

    // ---------- removing ----------

    @Test
    void removingAMemberClosesTheGapAndKeepsHistory() throws Exception {
        enroll(youth, alice, bob, carol);

        remove(amy, youth, bob).andExpect(status().isNoContent());

        assertThat(names(youth)).containsExactly("Alice", "Carol");
        assertContiguous(youth);

        // the membership row is closed, not deleted
        assertThat(roomMembers.findAll()).hasSize(3);
        assertThat(roomMembers.findAll().stream().filter(rm -> rm.getRemovedAt() != null)).hasSize(1);
        // and the person's account is untouched
        assertThat(users.findById(bob.getId()).orElseThrow().getStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    void removingTheFirstAndTheLastMemberWorks() throws Exception {
        enroll(youth, alice, bob, carol);

        remove(amy, youth, alice).andExpect(status().isNoContent());
        assertThat(names(youth)).containsExactly("Bob", "Carol");
        assertContiguous(youth);

        remove(amy, youth, carol).andExpect(status().isNoContent());
        assertThat(names(youth)).containsExactly("Bob");
        assertContiguous(youth);

        remove(amy, youth, bob).andExpect(status().isNoContent());
        assertThat(names(youth)).isEmpty();
    }

    @Test
    void severalRemovalsInARowKeepTheNumbersContiguous() throws Exception {
        List<User> six = IntStream.rangeClosed(1, 6)
                .mapToObj(i -> member("m" + i + "@test.com", "M" + i, victory)).toList();
        enroll(youth, six.toArray(new User[0]));

        remove(amy, youth, six.get(1)).andExpect(status().isNoContent());   // M2
        remove(amy, youth, six.get(3)).andExpect(status().isNoContent());   // M4
        remove(amy, youth, six.get(5)).andExpect(status().isNoContent());   // M6

        assertThat(names(youth)).containsExactly("M1", "M3", "M5");
        assertContiguous(youth);
    }

    @Test
    void aRemovedMemberCanBeAddedAgainAtTheEnd() throws Exception {
        enroll(youth, alice, bob, carol);
        remove(amy, youth, alice).andExpect(status().isNoContent());

        addExisting(amy, youth, alice)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.participationNumber").value(3));

        assertThat(names(youth)).containsExactly("Bob", "Carol", "Alice");
        assertContiguous(youth);
    }

    @Test
    void removingSomeoneWhoIsNotInTheRoomOrTwiceIsA404() throws Exception {
        enroll(youth, alice);

        remove(amy, youth, bob).andExpect(status().isNotFound());           // never in the room
        remove(amy, youth, alice).andExpect(status().isNoContent());
        remove(amy, youth, alice).andExpect(status().isNotFound());         // already removed
    }

    @Test
    void anotherLeadersRoomCannotBeChangedByRemoving() throws Exception {
        User zed = member("zed@test.com", "Zed", grace);
        enroll(benRoom, zed);

        remove(amy, benRoom, zed).andExpect(status().isNotFound());
        assertThat(names(benRoom)).containsExactly("Zed");
    }

    // ---------- archived rooms ----------

    @Test
    void anArchivedRoomCannotBeReorderedOrEmptied() throws Exception {
        enroll(youth, alice, bob);
        youth.archive(Instant.now());
        rooms.save(youth);

        reorder(amy, youth, bob, alice).andExpect(status().isUnprocessableEntity());
        remove(amy, youth, alice).andExpect(status().isUnprocessableEntity());

        assertThat(names(youth)).containsExactly("Alice", "Bob");
    }

    // ---------- roles ----------

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "PASTOR", "MEMBER"})
    void onlyLeadersCanReorderOrRemove(Role role) throws Exception {
        String token = bearer(role);

        mockMvc.perform(put(members(youth) + "/order").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"memberIds\":[]}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(members(youth) + "/1").header("Authorization", token))
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

    private void enroll(Room room, User... people) {
        for (User person : people) {
            roomMemberService.enroll(room.getLeaderId(), room.getId(), person.getId());
        }
    }

    private List<String> names(Room room) {
        return roomMembers.roster(room.getId()).stream().map(RoomMemberResponse::fullName).toList();
    }

    // The active numbers must always be exactly 1..n.
    private void assertContiguous(Room room) {
        List<Integer> numbers = roomMembers.roster(room.getId()).stream()
                .map(RoomMemberResponse::participationNumber).toList();
        assertThat(numbers).isEqualTo(IntStream.rangeClosed(1, numbers.size()).boxed().toList());
    }

    private ResultActions reorder(User leader, Room room, User... people) throws Exception {
        return reorderIds(leader, room, Arrays.stream(people).map(User::getId).toArray(Long[]::new));
    }

    private ResultActions reorderIds(User leader, Room room, Long... memberIds) throws Exception {
        String ids = Arrays.stream(memberIds).map(String::valueOf).collect(Collectors.joining(","));
        return mockMvc.perform(put(members(room) + "/order").header("Authorization", bearer(leader))
                .contentType(MediaType.APPLICATION_JSON).content("{\"memberIds\":[%s]}".formatted(ids)));
    }

    private ResultActions remove(User leader, Room room, User person) throws Exception {
        return mockMvc.perform(delete(members(room) + "/" + person.getId())
                .header("Authorization", bearer(leader)));
    }

    private ResultActions addExisting(User leader, Room room, User person) throws Exception {
        return mockMvc.perform(post(members(room)).header("Authorization", bearer(leader))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":%d}".formatted(person.getId())));
    }

    private static String members(Room room) {
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