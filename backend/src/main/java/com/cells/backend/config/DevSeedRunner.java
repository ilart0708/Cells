package com.cells.backend.config;

import com.cells.backend.user.Role;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * LOCAL DEVELOPMENT ONLY. Creates one test user per role so access rules can be tested
 * before invitations exist (Phase 3). Does nothing unless DEV_SEED_PASSWORD is set.
 */
@Component
@Profile("local")
public class DevSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String password;

    public DevSeedRunner(UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         @Value("${app.dev-seed.password:}") String password) {
        this.userRepository = userRepository;
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
        seed("pastor@example.com", "Test Pastor", Role.PASTOR);
        seed("leader@example.com", "Test Leader", Role.LEADER);
        seed("member@example.com", "Test Member", Role.MEMBER);
    }

    private void seed(String email, String fullName, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        userRepository.save(new User(email, passwordEncoder.encode(password), fullName, role));
        log.info("Dev seed user created: {} ({})", email, role);
    }
}