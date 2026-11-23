package com.cells.backend.cycle;

import com.cells.backend.devotion.DevotionRepository;
import com.cells.backend.devotion.DevotionStatus;
import com.cells.backend.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * What happens when a new day begins. It deliberately changes NO history: submissions, exemptions and EXP are
 * keyed by date, so yesterday's exemptions simply stop applying and the new day starts clean by itself.
 * The only job left is to tell members that the devotion prepared for today is now open.
 *
 * Every step is idempotent (the notification table's unique key), so running this twice, or again after a
 * restart, is harmless.
 */
@Service
public class CycleTransitionService {

    private static final Logger log = LoggerFactory.getLogger(CycleTransitionService.class);

    private final DevotionRepository devotionRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    public CycleTransitionService(DevotionRepository devotionRepository,
                                  NotificationService notificationService,
                                  Clock clock) {
        this.devotionRepository = devotionRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /** Not transactional on purpose: each room is handled in its own transaction, so one bad room cannot hold back the rest. */
    public void openToday() {
        LocalDate today = LocalDate.now(clock);
        List<Long> devotionIds = devotionRepository.findIdsByDevotionDateAndStatus(today, DevotionStatus.ACTIVE);
        int failed = 0;
        for (Long devotionId : devotionIds) {
            try {
                notificationService.devotionOpened(devotionId);
            } catch (RuntimeException e) {
                failed++;
                log.warn("Could not queue notifications for devotion {} on {}: {}", devotionId, today, e.toString());
            }
        }
        log.info("Daily cycle {} opened: {} devotion(s) checked, {} failed.", today, devotionIds.size(), failed);
    }
}
