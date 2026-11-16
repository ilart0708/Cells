package com.cells.backend.probe;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** TEMPORARY: proves role checks work. Delete at the end of Phase 2. */
@RestController
@RequestMapping("/api/v1/probe")
public class RoleProbeController {

    public record ProbeResponse(String area, String userId, List<String> roles) {
    }

    @GetMapping("/any")
    public ProbeResponse any(@AuthenticationPrincipal Jwt jwt) {
        return respond("any logged-in user", jwt);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ProbeResponse admin(@AuthenticationPrincipal Jwt jwt) {
        return respond("administrator area", jwt);
    }

    @GetMapping("/pastor")
    @PreAuthorize("hasRole('PASTOR')")
    public ProbeResponse pastor(@AuthenticationPrincipal Jwt jwt) {
        return respond("pastor area", jwt);
    }

    @GetMapping("/leader")
    @PreAuthorize("hasRole('LEADER')")
    public ProbeResponse leader(@AuthenticationPrincipal Jwt jwt) {
        return respond("leader area", jwt);
    }

    @GetMapping("/member")
    @PreAuthorize("hasRole('MEMBER')")
    public ProbeResponse member(@AuthenticationPrincipal Jwt jwt) {
        return respond("member area", jwt);
    }

    private ProbeResponse respond(String area, Jwt jwt) {
        return new ProbeResponse(area, jwt.getSubject(), jwt.getClaimAsStringList("roles"));
    }
}