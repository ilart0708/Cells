package com.cells.backend.leader;

import com.cells.backend.common.BusinessRuleException;
import com.cells.backend.common.PageResponse;
import com.cells.backend.common.ResourceNotFoundException;
import com.cells.backend.user.User;
import com.cells.backend.user.UserRepository;
import com.cells.backend.user.UserStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaderService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CellGroupLeaderRepository leaderRepository;
    private final PastorLeaderRepository pastorLeaderRepository;
    private final UserRepository userRepository;

    public LeaderService(CellGroupLeaderRepository leaderRepository,
                         PastorLeaderRepository pastorLeaderRepository,
                         UserRepository userRepository) {
        this.leaderRepository = leaderRepository;
        this.pastorLeaderRepository = pastorLeaderRepository;
        this.userRepository = userRepository;
    }

    // ---------- administrator: every leader ----------

    @Transactional(readOnly = true)
    public PageResponse<LeaderResponse> listAll(String query, int page, int size) {
        return PageResponse.from(leaderRepository.search(pattern(query), pageable(page, size)));
    }

    @Transactional
    public LeaderResponse setStatusAsAdmin(Long leaderId, UserStatus status) {
        if (!leaderRepository.existsById(leaderId)) {
            throw notFound();
        }
        return applyStatus(leaderId, status);
    }

    // ---------- pastor: only their own leaders ----------

    @Transactional(readOnly = true)
    public PageResponse<LeaderResponse> listForPastor(Long pastorId, String query, int page, int size) {
        return PageResponse.from(
                leaderRepository.searchForPastor(pattern(query), pastorId, pageable(page, size)));
    }

    @Transactional(readOnly = true)
    public LeaderResponse getForPastor(Long pastorId, Long leaderId) {
        return leaderRepository.findResponseByIdForPastor(leaderId, pastorId)
                .orElseThrow(LeaderService::notFound);
    }

    @Transactional
    public LeaderResponse setStatusForPastor(Long pastorId, Long leaderId, UserStatus status) {
        // Ownership first: another pastor's leader looks exactly like a leader that does not exist.
        if (!pastorLeaderRepository.existsByLeaderIdAndPastorId(leaderId, pastorId)) {
            throw notFound();
        }
        return applyStatus(leaderId, status);
    }

    // ---------- shared ----------

    private LeaderResponse applyStatus(Long leaderId, UserStatus status) {
        if (status == UserStatus.PENDING) {
            throw new BusinessRuleException("Status must be ACTIVE or DISABLED.");
        }
        User user = userRepository.findById(leaderId).orElseThrow(LeaderService::notFound);
        if (status == UserStatus.ACTIVE) {
            user.activate();
        } else {
            user.disable();
        }
        return leaderRepository.findResponseById(leaderId).orElseThrow(LeaderService::notFound);
    }

    private static PageRequest pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    private static String pattern(String query) {
        return "%" + (query == null ? "" : query.trim().toLowerCase()) + "%";
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Leader not found.");
    }
}