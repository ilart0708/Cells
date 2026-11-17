package com.cells.backend.config;

import com.cells.backend.church.Church;
import com.cells.backend.church.ChurchRepository;
import com.cells.backend.pastor.Pastor;
import com.cells.backend.pastor.PastorRepository;
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
 * LOCAL DEVELOPMENT ONLY. Creates a demo church and one test user per role so access rules
 * can be tested. Does nothing unless DEV_SEED_PASSWORD is set.
 */
@Component
@Profile("local")
public class DevSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

    private final UserRepository userRepository;
    private final ChurchRepository churchRepository;
    private final PastorRepository pastorRepository;
    private final PasswordEncoder passwordEncoder;
    private final String password;

    public DevSeedRunner(UserRepository userRepository,
                         ChurchRepository churchRepository,
                         PastorRepository pastorRepository,
                         PasswordEncoder passwordEncoder,
                         @Value("${app.dev-seed.password:}") String password) {
        this.userRepository = userRepository;
        this.churchRepository = churchRepository;
        this.pastorRepository = pastorRepository;
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

        seed("leader@example.com", "Test Leader", Role.LEADER);
        seed("member@example.com", "Test Member", Role.MEMBER);
    }

    private User seed(String email, String fullName, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Dev seed user created: {} ({})", email, role);
            return userRepository.save(new User(email, passwordEncoder.encode(password), fullName, role));
        });
    }
}