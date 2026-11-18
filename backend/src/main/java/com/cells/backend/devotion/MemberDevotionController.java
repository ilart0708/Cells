package com.cells.backend.devotion;

import com.cells.backend.common.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Scoped to the logged-in member: the id always comes from the token. */
@RestController
@RequestMapping("/api/v1/member/rooms/{roomId}/devotions")
@PreAuthorize("hasRole('MEMBER')")
public class MemberDevotionController {

    private final MemberDevotionService memberDevotionService;

    public MemberDevotionController(MemberDevotionService memberDevotionService) {
        this.memberDevotionService = memberDevotionService;
    }

    /** Today's devotion for this room (devotion is null when the leader has not posted one). */
    @GetMapping("/today")
    public TodayDevotionResponse today(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId) {
        return memberDevotionService.today(memberId(jwt), roomId);
    }

    @GetMapping
    public PageResponse<MemberDevotionResponse> list(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return memberDevotionService.list(memberId(jwt), roomId, page, size);
    }

    @GetMapping("/{id}")
    public MemberDevotionResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId, @PathVariable Long id) {
        return memberDevotionService.get(memberId(jwt), roomId, id);
    }

    private static Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}