package com.cells.backend.invitation;

import com.cells.backend.auth.AuthResult;
import com.cells.backend.auth.AuthService;
import com.cells.backend.church.Church;
import com.cells.backend.church.ChurchRepository;
import com.cells.backend.church.ChurchStatus;
import com.cells.backend.common.BusinessRuleException;
import com.cells.backend.common.DuplicateResourceException;
import com.cells.backend.common.InvalidInvitationException;
import com.cells.backend.common.PageResponse;
import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.common.TokenHasher;
import com.cells.backend.config.InvitationProperties;
import com.cells.backend.leader.CellGroupLeader;
import com.cells.backend.leader.CellGroupLeaderRepository;
import com.cells.backend.leader.PastorLeader;
import com.cells.backend.leader.PastorLeaderRepository;
import com.cells.backend.pastor.Pastor;
import com.cells.backend.pastor.PastorRepository;
import com.cells.backend.user.Role;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class InvitationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final InvitationRepository invitationRepository;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final PastorRepository pastorRepository;
    private final CellGroupLeaderRepository leaderRepository;
    private final PastorLeaderRepository pastorLeaderRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final InvitationProperties properties;

    public InvitationService(InvitationRepository invitationRepository,
                             ChurchRepository churchRepository,
                             UserRepository userRepository,
                             PastorRepository pastorRepository,
                             CellGroupLeaderRepository leaderRepository,
                             PastorLeaderRepository pastorLeaderRepository,
                             PasswordEncoder passwordEncoder,
                             AuthService authService,
                             InvitationProperties properties) {
        this.invitationRepository = invitationRepository;
        this.churchRepository = churchRepository;
        this.userRepository = userRepository;
        this.pastorRepository = pastorRepository;
        this.leaderRepository = leaderRepository;
        this.pastorLeaderRepository = pastorLeaderRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.properties = properties;
    }

    // ---------- creating ----------

    @Transactional
    public InvitationResponse invitePastor(Long adminId, PastorInvitationRequest request) {
        Church church = churchRepository.findById(request.churchId())
                .orElseThrow(() -> new ResourceNotFoundException("Church not found."));
        return createInvitation(adminId, request.email(), request.fullName(), Role.PASTOR, church);
    }

    @Transactional
    public InvitationResponse inviteLeader(Long pastorId, LeaderInvitationRequest request) {
        Pastor pastor = pastorRepository.findById(pastorId)
                .orElseThrow(() -> new BusinessRuleException("Your account is not linked to a church."));
        return createInvitation(pastorId, request.email(), request.fullName(), Role.LEADER, pastor.getChurch());
    }

    private InvitationResponse createInvitation(Long inviterId, String rawEmail, String fullName,
                                                Role role, Church church) {
        if (church.getStatus() != ChurchStatus.ACTIVE) {
            throw new BusinessRuleException("This church is not active.");
        }

        String email = rawEmail.trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("A user with this email already exists.");
        }

        Instant now = Instant.now();
        // A new link replaces this inviter's older pending link for the same email, nobody else's.
        invitationRepository.revokePendingForEmail(email, inviterId, now);

        String token = TokenHasher.newToken();
        Invitation invitation = invitationRepository.save(new Invitation(
                TokenHasher.sha256(token), email, fullName.trim(),
                role, church, inviterId, now.plus(properties.ttl())));

        return InvitationResponse.from(invitation, now, inviteUrl(token));
    }

    // ---------- listing and revoking ----------

    @Transactional(readOnly = true)
    public PageResponse<InvitationResponse> list(int page, int size) {
        Instant now = Instant.now();
        return PageResponse.from(invitationRepository.findAll(pageable(page, size))
                .map(invitation -> InvitationResponse.from(invitation, now, null)));
    }

    @Transactional(readOnly = true)
    public PageResponse<InvitationResponse> listForInviter(Long inviterId, int page, int size) {
        Instant now = Instant.now();
        Page<Invitation> result = invitationRepository.findByInvitedBy(inviterId, pageable(page, size));
        return PageResponse.from(result.map(invitation -> InvitationResponse.from(invitation, now, null)));
    }

    @Transactional
    public void revoke(Long id) {
        Invitation invitation = invitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found."));
        revokeInvitation(invitation);
    }

    @Transactional
    public void revokeOwn(Long id, Long inviterId) {
        Invitation invitation = invitationRepository.findByIdAndInvitedBy(id, inviterId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found."));
        revokeInvitation(invitation);
    }

    private void revokeInvitation(Invitation invitation) {
        Instant now = Instant.now();
        if (invitation.getStatus(now) == InvitationStatus.ACCEPTED) {
            throw new BusinessRuleException("An accepted invitation cannot be revoked.");
        }
        invitation.markRevoked(now);
    }

    // ---------- the invited person ----------

    @Transactional(readOnly = true)
    public InvitationPreviewResponse preview(String token) {
        Invitation invitation = invitationRepository.findByTokenHash(TokenHasher.sha256(token))
                .orElseThrow(InvalidInvitationException::new);
        if (invitation.getStatus(Instant.now()) != InvitationStatus.PENDING) {
            throw new InvalidInvitationException();
        }
        return new InvitationPreviewResponse(
                invitation.getEmail(), invitation.getFullName(), invitation.getRole(),
                invitation.getChurch().getName(), invitation.getExpiresAt());
    }

    @Transactional
    public AuthResult accept(AcceptInvitationRequest request) {
        Invitation invitation = invitationRepository
                .findByTokenHashForUpdate(TokenHasher.sha256(request.token()))
                .orElseThrow(InvalidInvitationException::new);

        Instant now = Instant.now();
        if (invitation.getStatus(now) != InvitationStatus.PENDING) {
            throw new InvalidInvitationException();
        }
        if (userRepository.existsByEmail(invitation.getEmail())) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }

        User user = userRepository.save(new User(
                invitation.getEmail(),
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                invitation.getRole()));
        invitation.markAccepted(now);

        switch (invitation.getRole()) {
            case PASTOR -> pastorRepository.save(new Pastor(user.getId(), invitation.getChurch()));
            case LEADER -> {
                if (!pastorRepository.existsById(invitation.getInvitedBy())) {
                    throw new InvalidInvitationException();
                }
                leaderRepository.save(new CellGroupLeader(user.getId(), invitation.getChurch()));
                pastorLeaderRepository.save(new PastorLeader(user.getId(), invitation.getInvitedBy()));
            }
            default -> {
                // MEMBER invitations arrive in Phase 4, together with rooms.
            }
        }

        return authService.startSession(user);
    }

    private static PageRequest pageable(int page, int size) {
        return PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private String inviteUrl(String token) {
        String base = properties.baseUrl().replaceAll("/+$", "");
        return base + "/accept-invitation#token=" + token;
    }
}