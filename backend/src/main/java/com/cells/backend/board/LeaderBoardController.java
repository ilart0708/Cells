package com.cells.backend.board;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Scoped to the logged-in leader: the id always comes from the token. */
@RestController
@RequestMapping("/api/v1/leader/rooms/{roomId}/board")
@PreAuthorize("hasRole('LEADER')")
public class LeaderBoardController {

    private final BoardService boardService;

    public LeaderBoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    /** The same board members see. Members who chose to hide are not listed here either. */
    @GetMapping
    public GrowthBoardResponse board(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId) {
        return boardService.forLeader(Long.valueOf(jwt.getSubject()), roomId);
    }
}