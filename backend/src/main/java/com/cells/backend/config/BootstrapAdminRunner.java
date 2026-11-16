package com.cells.backend.config;

import com.cells.backend.user.Role;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final BootstrapAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapAdminRunner(BootstrapAdminProperties properties,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (isBlank(properties.email()) || isBlank(properties.password())) {
            log.info("Bootstrap admin skipped: BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD not set");
            return;
        }

        String email = properties.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            log.info("Bootstrap admin already exists: {}", email);
            return;
        }

        if (properties.password().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        User admin = new User(
                email,
                passwordEncoder.encode(properties.password()),
                properties.fullName(),
                Role.ADMIN);
        userRepository.save(admin);
        log.info("Bootstrap admin created: {}", email);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}