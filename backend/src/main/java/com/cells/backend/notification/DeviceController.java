package com.cells.backend.notification;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * The mobile app registers its push token after login and removes it at logout. The user id always
 * comes from the token, never from the request.
 */
@RestController
@RequestMapping("/api/v1/devices")
@PreAuthorize("isAuthenticated()")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeviceRequest request) {
        deviceService.register(Long.valueOf(jwt.getSubject()), request);
    }

    @DeleteMapping("/{token:.+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@AuthenticationPrincipal Jwt jwt, @PathVariable String token) {
        deviceService.unregister(Long.valueOf(jwt.getSubject()), token);
    }
}
