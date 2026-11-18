package com.cells.backend.clock;

import com.cells.backend.devotion.DevotionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/clock")
@PreAuthorize("isAuthenticated()")
public class ClockController {

    private final Clock clock;

    public ClockController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping("/today")
    public ClockResponse today() {
        LocalDate today = LocalDate.now(clock);
        return new ClockResponse(today, clock.getZone().getId(), today.plusDays(DevotionService.MAX_DAYS_AHEAD));
    }
}