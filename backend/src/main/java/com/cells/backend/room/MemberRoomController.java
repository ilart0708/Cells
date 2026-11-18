package com.cells.backend.room;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Scoped to the logged-in member: the id always comes from the token. */
@RestController
@RequestMapping("/api/v1/member/rooms")
@PreAuthorize("hasRole('MEMBER')")
public class MemberRoomController {

    private final MemberRoomService memberRoomService;

    public MemberRoomController(MemberRoomService memberRoomService) {
        this.memberRoomService = memberRoomService;
    }

    @GetMapping
    public List<MemberRoomResponse> myRooms(@AuthenticationPrincipal Jwt jwt) {
        return memberRoomService.myRooms(memberId(jwt));
    }

    @GetMapping("/{roomId}")
    public MemberRoomDetailResponse detail(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId) {
        return memberRoomService.detail(memberId(jwt), roomId);
    }

    private static Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}