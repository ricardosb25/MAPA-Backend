package com.mapa.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailDeliveryStatusTest {

    private static final Instant START_INSTANT = Instant.parse("2026-01-01T00:00:00Z");

    private static class MutableClock extends Clock {

        private Instant currentInstant;

        private MutableClock(Instant initialInstant) {
            this.currentInstant = initialInstant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }

    @Test
    void shouldBecomeUnavailableAfterMarkingFailure() {
        EmailDeliveryStatus emailDeliveryStatus = new EmailDeliveryStatus(
                Clock.fixed(START_INSTANT, ZoneOffset.UTC));

        assertFalse(emailDeliveryStatus.isUnavailable());

        emailDeliveryStatus.markFailure();

        assertTrue(emailDeliveryStatus.isUnavailable());
    }

    @Test
    void shouldBecomeAvailableAgainAfterFailureWindowExpires() {
        MutableClock mutableClock = new MutableClock(START_INSTANT);
        EmailDeliveryStatus emailDeliveryStatus = new EmailDeliveryStatus(mutableClock);

        emailDeliveryStatus.markFailure();
        assertTrue(emailDeliveryStatus.isUnavailable());

        mutableClock.currentInstant = START_INSTANT
                .plus(EmailDeliveryStatus.FAILURE_WINDOW)
                .plusSeconds(1);
        assertFalse(emailDeliveryStatus.isUnavailable());
    }

    @Test
    void shouldRestoreAvailabilityWhenCleared() {
        EmailDeliveryStatus emailDeliveryStatus = new EmailDeliveryStatus(
                Clock.fixed(START_INSTANT, ZoneOffset.UTC));

        emailDeliveryStatus.markFailure();
        emailDeliveryStatus.clear();

        assertFalse(emailDeliveryStatus.isUnavailable());
    }
}
