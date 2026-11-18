package com.cells.backend.config;

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
import com.cells.backend.room.Room;
import com.cells.backend.room.RoomMemberRepository;
import com.cells.backend.room.RoomMemberResponse;
import com.cells.backend.room.RoomMemberService;
import com.cells.backend.room.RoomRepository;
import com.cells.backend.room.RoomStatus;
import com.cells.backend.user.Role;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * LOCAL DEVELOPMENT ONLY. Creates a demo church, one test user per role, and a demo room with
 * three members so screens and access rules can be tried. Does nothing unless DEV_SEED_PASSWORD is set.
 */
@Component
@Order(1)
@Profile("local")
public class DevSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

    private static final String[][] DEMO_MEMBERS = {
            {"member@example.com", "Test Member"},
            {"member2@example.com", "Second Member"},
            {"member3@example.com", "Third Member"},
    };

    private final UserRepository userRepository;
    private final ChurchRepository churchRepository;
    private final PastorRepository pastorRepository;
    private final CellGroupLeaderRepository leaderRepository;
    private final PastorLeaderRepository pastorLeaderRepository;
    private final MemberRepository memberRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomMemberService roomMemberService;
    private final PasswordEncoder passwordEncoder;
    private final String password;

    public DevSeedRunner(UserRepository userRepository,
                         ChurchRepository churchRepository,
                         PastorRepository pastorRepository,
                         CellGroupLeaderRepository leaderRepository,
                         PastorLeaderRepository pastorLeaderRepository,
                         MemberRepository memberRepository,
                         RoomRepository roomRepository,
                         RoomMemberRepository roomMemberRepository,
                         RoomMemberService roomMemberService,
                         PasswordEncoder passwordEncoder,
                         @Value("${app.dev-seed.password:}") String password) {
        this.userRepository = userRepository;
        this.churchRepository = churchRepository;
        this.pastorRepository = pastorRepository;
        this.leaderRepository = leaderRepository;
        this.pastorLeaderRepository = pastorLeaderRepository;
        this.memberRepository = memberRepository;
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.roomMemberService = roomMemberService;
        this.passwordEncoder = passwordEncoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password == null || password.length() < 12) {
            log.info("Dev seed skipped: set DEV_SEED_PASSWORD (12+ characters) to create test users");
            return;
        }

        Church church = churchRepository.findByNameIgnoreCase("Demo Church")
                .orElseGet(() -> churchRepository.save(new Church("Demo Church")));

        User pastor = seed("pastor@example.com", "Test Pastor", Role.PASTOR);
        if (!pastorRepository.existsById(pastor.getId())) {
            pastorRepository.save(new Pastor(pastor.getId(), church));
            log.info("Dev seed: {} linked to {}", pastor.getEmail(), church.getName());
        }

        User leader = seed("leader@example.com", "Test Leader", Role.LEADER);
        if (!leaderRepository.existsById(leader.getId())) {
            leaderRepository.save(new CellGroupLeader(leader.getId(), church));
            pastorLeaderRepository.save(new PastorLeader(leader.getId(), pastor.getId()));
            log.info("Dev seed: {} linked to {} under pastor {}",
                    leader.getEmail(), church.getName(), pastor.getEmail());
        }

        Room room = roomRepository
                .findByLeaderIdAndStatusAndNameIgnoreCase(leader.getId(), RoomStatus.ACTIVE, "Demo Room")
                .orElseGet(() -> {
                    log.info("Dev seed: room 'Demo Room' created for {}", leader.getEmail());
                    return roomRepository.save(new Room(church, leader.getId(), "Demo Room"));
                });

        for (String[] demo : DEMO_MEMBERS) {
            User user = seed(demo[0], demo[1], Role.MEMBER);
            if (!memberRepository.existsById(user.getId())) {
                memberRepository.save(new Member(user.getId(), church));
            }
            if (!roomMemberRepository.existsByRoomIdAndMemberIdAndRemovedAtIsNull(room.getId(), user.getId())) {
                RoomMemberResponse enrolled = roomMemberService.enroll(leader.getId(), room.getId(), user.getId());
                log.info("Dev seed: {} joined Demo Room as #{}", user.getEmail(), enrolled.participationNumber());
            }
        }
    }

    private User seed(String email, String fullName, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Dev seed user created: {} ({})", email, role);
            return userRepository.save(new User(email, passwordEncoder.encode(password), fullName, role));
        });
    }
}