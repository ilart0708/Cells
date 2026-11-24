package com.cells.backend.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class DeviceService {

    private final DeviceTokenRepository repository;
    private final Clock clock;

    public DeviceService(DeviceTokenRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Registering the same token again is harmless; a token last used by someone else moves to this account. */
    @Transactional
    public void register(Long userId, DeviceRequest request) {
        String token = request.token().strip();
        DeviceToken existing = repository.findByToken(token).orElse(null);
        if (existing == null) {
            repository.save(new DeviceToken(userId, token, request.platform(), clock.instant()));
        } else {
            existing.useBy(userId, request.platform(), clock.instant());
        }
    }

    /** Removing a token that is not there (or is someone else's) is harmless. */
    public void unregister(Long userId, String token) {
        repository.deleteOwn(token.strip(), userId);
    }
}
