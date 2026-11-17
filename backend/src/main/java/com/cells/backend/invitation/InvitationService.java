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
import com.cells.backend.member.Member;
import com.cells.backend.member.MemberRepository;
import com.cells.backend.pastor.Pastor;
import com.cells.backend.pastor.PastorRepository;
import com.cells.backend.room.Room;
import com.cells.backend.room.RoomMemberService;
import com.cells.backend.room.RoomRepository;
import com.cells.backend.room.RoomStatus;
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
    private final MemberRepository memberRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberService roomMemberService;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final InvitationProperties properties;

    public InvitationService(InvitationRepository invitationRepository,
                             ChurchRepository churchRepository,
                             UserRepository userRepository,
                             PastorRepository pastorRepository,
                             CellGroupLeaderRepository leaderRepository,
                             PastorLeaderRepository pastorLeaderRepository,
                             MemberRepository memberRepository,
                             RoomRepository roomRepository,
                             RoomMemberService roomMemberService,
                             PasswordEncoder passwordEncoder,
                             AuthService authService,
                             InvitationProperties properties) {
        this.invitationRepository = invitationRepository;
        this.churchRepository = churchRepository;
        this.userRepository = userRepository;
        this.pastorRepository = pastorRepository;
        this.leaderRepository = leaderRepository;
        this.pastorLeaderRepository = pastorLeaderRepository;
        this.memberRepository = memberRepository;
        this.roomRepository = roomRepository;
        this.roomMemberService = roomMemberService;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.properties = properties;
    }

    // ---------- creating ----------

    @Transactional
    public InvitationResponse invitePastor(Long adminId, PastorInvitationRequest request) {
        Church church = churchRepository.findById(request.churchId())
                .orElseThrow(() -> new ResourceNotFoundException("Church not found."));
        return createInvitation(adminId, request.email(), request.fullName(), Role.PASTOR, church, null);
    }

    @Transactional
    public InvitationResponse inviteLeader(Long pastorId, LeaderInvitationRequest request) {
        Pastor pastor = pastorRepository.findById(pastorId)
                .orElseThrow(() -> new BusinessRuleException("Your account is not linked to a church."));
        return createInvitation(pastorId, request.email(), request.fullName(), Role.LEADER, pastor.getChurch(), null);
    }

    @Transactional
    public InvitationResponse inviteMember(Long leaderId, Long roomId, MemberInvitationRequest request) {
        // Another leader's room looks exactly like a room that does not exist.
        Room room = roomRepository.findByIdAndLeaderId(roomId, leaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new BusinessRuleException("Archived rooms cannot get new members.");
        }

        String email = request.email().trim().toLowerCase();
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            // Only a member of THIS church is described. Everyone else gets the same vague answer,
            // so a leader cannot find out who has accounts elsewhere.
            boolean sameChurchMember = memberRepository.findById(existing.getId())
                    .map(m -> m.getChurch().getId().equals(room.getChurch().getId()))
                    .orElse(false);
            throw new DuplicateResourceException(sameChurchMember
                    ? "This person already has an account. Add them from the list of church members."
                    : "This email address cannot be invited to the room.");
        }

        return createInvitation(leaderId, email, request.fullName(), Role.MEMBER, room.getChurch(), room);
    }

    private InvitationResponse createInvitation(Long inviterId, String rawEmail, String fullName,
                                                Role role, Church church, Room room) {
        if (church.getStatus() != ChurchStatus.ACTIVE) {
            throw new BusinessRuleException("This church is not active.");
        }

        String email = rawEmail.trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("A user with this email already exists.");
        }

        Instant now = Instant.now();
        // A new link replaces this inviter's older pending link for the same email (and the same room).
        if (room == null) {
            invitationRepository.revokePendingForEmail(email, inviterId, now);
        } else {
            invitationRepository.revokePendingForEmailInRoom(email, inviterId, room.getId(), now);
        }

        String token = TokenHasher.newToken();
        Invitation invitation = invitationRepository.save(new Invitation(
                TokenHasher.sha256(token), email, fullName.trim(),
                role, church, room, inviterId, now.plus(properties.ttl())));

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

    @Transactional(readOnly = true)
    public PageResponse<InvitationResponse> listForRoom(Long leaderId, Long roomId, int page, int size) {
        roomRepository.findByIdAndLeaderId(roomId, leaderId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found."));
        Instant now = Instant.now();
        Page<Invitation> result = invitationRepository.findByRoom_IdAndInvitedBy(
                roomId, leaderId, pageable(page, size));
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

    @Transactional
    public void revokeForRoom(Long leaderId, Long roomId, Long id) {
        Invitation invitation = invitationRepository.findByIdAndInvitedByAndRoom_Id(id, leaderId, roomId)
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
        if (invitation.getStatus(Instant.now()) != InvitationStatus.PENDING || !roomIsOpen(invitation)) {
            throw new InvalidInvitationException();
        }

        Room room = invitation.getRoom();
        String leaderName = room == null ? null
                : userRepository.findById(invitation.getInvitedBy()).map(User::getFullName).orElse(null);

        return new InvitationPreviewResponse(
                invitation.getEmail(), invitation.getFullName(), invitation.getRole(),
                invitation.getChurch().getName(),
                room == null ? null : room.getName(), leaderName,
                invitation.getExpiresAt());
    }

    @Transactional
    public AuthResult accept(AcceptInvitationRequest request) {
        Invitation invitation = invitationRepository
                .findByTokenHashForUpdate(TokenHasher.sha256(request.token()))
                .orElseThrow(InvalidInvitationException::new);

        Instant now = Instant.now();
        if (invitation.getStatus(now) != InvitationStatus.PENDING || !roomIsOpen(invitation)) {
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
            case MEMBER -> {
                memberRepository.saveAndFlush(new Member(user.getId(), invitation.getChurch()));
                // Same path as "add an existing member": it locks the room and hands out the next number.
                roomMemberService.enroll(invitation.getInvitedBy(), invitation.getRoom().getId(), user.getId());
            }
            default -> throw new IllegalStateException("Unsupported invitation role: " + invitation.getRole());
        }

        return authService.startSession(user);
    }

    // A member invitation is only usable while its room is active.
    private static boolean roomIsOpen(Invitation invitation) {
        if (invitation.getRole() != Role.MEMBER) {
            return true;
        }
        Room room = invitation.getRoom();
        return room != null && room.getStatus() == RoomStatus.ACTIVE;
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