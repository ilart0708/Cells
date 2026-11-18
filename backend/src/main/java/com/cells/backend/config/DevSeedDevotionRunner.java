package com.cells.backend.config;

import com.cells.backend.bible.BibleBook;
import com.cells.backend.devotion.Devotion;
import com.cells.backend.devotion.DevotionRepository;
import com.cells.backend.devotion.DevotionStatus;
import com.cells.backend.devotion.LearningLevel;
import com.cells.backend.room.Room;
import com.cells.backend.room.RoomRepository;
import com.cells.backend.room.RoomStatus;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/** LOCAL DEVELOPMENT ONLY. Gives Demo Room a devotion for today (runs after DevSeedRunner). */
@Component
@Order(2)
@Profile("local")
public class DevSeedDevotionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedDevotionRunner.class);

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final DevotionRepository devotionRepository;
    private final Clock clock;
    private final String password;

    public DevSeedDevotionRunner(UserRepository userRepository,
                                 RoomRepository roomRepository,
                                 DevotionRepository devotionRepository,
                                 Clock clock,
                                 @Value("${app.dev-seed.password:}") String password) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.devotionRepository = devotionRepository;
        this.clock = clock;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password == null || password.length() < 12) {
            return; // seeding is switched off
        }
        User leader = userRepository.findByEmail("leader@example.com").orElse(null);
        if (leader == null) {
            return;
        }
        Room room = roomRepository
                .findByLeaderIdAndStatusAndNameIgnoreCase(leader.getId(), RoomStatus.ACTIVE, "Demo Room")
                .orElse(null);
        if (room == null) {
            return;
        }

        LocalDate today = LocalDate.now(clock);
        if (devotionRepository.existsByRoomIdAndDevotionDateAndStatus(room.getId(), today, DevotionStatus.ACTIVE)) {
            return;
        }

        devotionRepository.save(new Devotion(
                room.getId(), today, BibleBook.JOHN, 3, 16, null,
                "God\u2019s Love", LearningLevel.BEGINNER,
                "Read the passage and share what you understand about God\u2019s love.",
                leader.getId()));
        log.info("Dev seed: today's devotion (John 3:16) created for Demo Room");
    }
}