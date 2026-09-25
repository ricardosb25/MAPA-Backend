package com.mapa.service;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class EmailDeliveryStatus {

    static final Duration FAILURE_WINDOW = Duration.ofSeconds(30);

    private final Clock clock;
    private final AtomicLong unavailableUntilEpochMillis = new AtomicLong(Long.MIN_VALUE);

    public EmailDeliveryStatus() {
        this(Clock.systemUTC());
    }

    EmailDeliveryStatus(Clock clock) {
        this.clock = clock;
    }

    public void markFailure() {
        unavailableUntilEpochMillis.set(clock.instant().plus(FAILURE_WINDOW).toEpochMilli());
    }

    public boolean isUnavailable() {
        return clock.instant().toEpochMilli() < unavailableUntilEpochMillis.get();
    }

    public void clear() {
        unavailableUntilEpochMillis.set(Long.MIN_VALUE);
    }
}
