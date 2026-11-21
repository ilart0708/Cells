package com.cells.backend.board;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Scoped to the logged-in member: the id always comes from the token. */
@RestController
@RequestMapping("/api/v1/member")
@PreAuthorize("hasRole('MEMBER')")
public class MemberBoardController {

    private final BoardService boardService;

    public MemberBoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    /** Growing Together for one of my rooms, for this month. */
    @GetMapping("/rooms/{roomId}/board")
    public GrowthBoardResponse board(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId) {
        return boardService.forMember(memberId(jwt), roomId);
    }

    @GetMapping("/settings/board")
    public BoardSettingResponse setting(@AuthenticationPrincipal Jwt jwt) {
        return boardService.setting(memberId(jwt));
    }

    /** Show me on my rooms' boards (true) or hide me (false). Repeating it is harmless. */
    @PutMapping("/settings/board")
    public BoardSettingResponse saveSetting(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody BoardSettingRequest request) {
        return boardService.saveSetting(memberId(jwt), request.showOnBoard());
    }

    private static Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}