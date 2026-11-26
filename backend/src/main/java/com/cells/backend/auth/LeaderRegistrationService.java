package com.cells.backend.auth;

import com.cells.backend.church.Church;
import com.cells.backend.church.ChurchRepository;
import com.cells.backend.common.DuplicateResourceException;
import com.cells.backend.leader.CellGroupLeader;
import com.cells.backend.leader.CellGroupLeaderRepository;
import com.cells.backend.user.Role;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sign-up for a leader whose church has no administrator to invite them.
 * Always creates a NEW church (never joins an existing one by name), so typing a church's name
 * gives no access to that church's members. The church is unverified until an administrator verifies it.
 */
@Service
public class LeaderRegistrationService {

    private final UserRepository userRepository;
    private final ChurchRepository churchRepository;
    private final CellGroupLeaderRepository leaderRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public LeaderRegistrationService(UserRepository userRepository,
                                     ChurchRepository churchRepository,
                                     CellGroupLeaderRepository leaderRepository,
                                     PasswordEncoder passwordEncoder,
                                     AuthService authService) {
        this.userRepository = userRepository;
        this.churchRepository = churchRepository;
        this.leaderRepository = leaderRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    @Transactional
    public AuthResult register(RegisterLeaderRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }

        Church church = churchRepository.save(
                Church.selfRegistered(request.churchName(), request.pastorName(), request.pastorEmail()));
        User user = userRepository.save(new User(
                email, passwordEncoder.encode(request.password()), request.fullName().trim(), Role.LEADER));
        leaderRepository.save(new CellGroupLeader(user.getId(), church));

        return authService.startSession(user);
    }
}
