package com.cells.backend.account;

import com.cells.backend.common.AccountNotActiveException;
import com.cells.backend.common.BusinessRuleException;
import com.cells.backend.common.InvalidCredentialsException;
import com.cells.backend.room.RoomMemberService;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import com.cells.backend.user.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Self-service account deletion (required by Google Play and promised in the privacy policy).
 *
 * <p>The user row itself stays, but anonymised, because rooms, devotions and statistics point at it. Everything that
 * identifies the person or that they wrote is deleted: name, email, password, answers, EXP, exemptions, summaries,
 * notifications, phone tokens, sessions and invitation records. Room history stays, with a "Deleted user" label.
 */
@Service
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoomMemberService roomMemberService;
    private final AccountDataRepository data;
    private final Clock clock;

    public AccountService(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          RoomMemberService roomMemberService,
                          AccountDataRepository data,
                          Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roomMemberService = roomMemberService;
        this.data = data;
        this.clock = clock;
    }

    @Transactional
    public void deleteOwnAccount(Long userId, String password) {
        User user = userRepository.findById(userId).orElseThrow(InvalidCredentialsException::new);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountNotActiveException();
        }
        // 422, not 401: the app treats a 401 as "session expired" and would log the person out instead of
        // showing the message.
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessRuleException("The password is incorrect.");
        }

        switch (user.getRole()) {
            case ADMIN -> throw new BusinessRuleException(
                    "Administrator accounts cannot be deleted here. Ask the platform owner.");
            case PASTOR -> {
                if (data.pastorHasLeaders(userId)) {
                    throw new BusinessRuleException(
                            "Cell group leaders are still assigned to you. Ask an administrator to move them first.");
                }
            }
            case LEADER -> {
                if (data.leaderHasActiveRooms(userId)) {
                    throw new BusinessRuleException(
                            "You still have active rooms. Archive each room first, then delete your account.");
                }
                data.deleteWrittenSummaries(userId);
                data.deleteLeaderAssignment(userId);
            }
            case MEMBER -> {
                roomMemberService.leaveAllRooms(userId);
                data.deleteMemberContent(userId);
            }
        }

        String email = user.getEmail();
        data.revokeOpenInvitationsSentBy(userId, clock.instant());
        data.deleteSignInAndDeviceData(userId);
        data.deleteOwnInvitations(email);
        // A random password nobody knows: the account can never sign in again, even if someone re-enables it.
        // Leaving rooms clears the persistence context, so load the row again before changing it.
        userRepository.findById(userId).orElseThrow(InvalidCredentialsException::new)
                .anonymize(passwordEncoder.encode(UUID.randomUUID().toString()));
    }
}
