package com.cells.backend.cycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Triggers CycleTransitionService at midnight in the platform's zone, and once on startup to catch up. */
@Component
public class CycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(CycleScheduler.class);

    private final CycleTransitionService transition;

    public CycleScheduler(CycleTransitionService transition) {
        this.transition = transition;
    }

    /** 12:00 AM in app.clock.zone (the same zone the Clock bean uses for "today"). */
    @Scheduled(cron = "0 0 0 * * *", zone = "${app.clock.zone}")
    public void midnight() {
        transition.openToday();
    }

    /** If the server was down or being deployed at midnight, the new day still gets its notifications. */
    @EventListener(ApplicationReadyEvent.class)
    public void catchUp() {
        try {
            transition.openToday();
        } catch (RuntimeException e) {
            log.warn("Startup catch-up of the daily cycle failed: {}", e.toString());
        }
    }
}
