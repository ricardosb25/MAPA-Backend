package com.mapa.service;

import com.mapa.config.properties.ResendProperties;
import com.mapa.exception.EmailDeliveryException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResendEmailServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T00:00:00Z");
    private static final String RECIPIENT_EMAIL = "ana@email.com";
    private static final int ALWAYS_FAIL = -1;

    private static class CountingResendEmailService extends ResendEmailService {

        private final AtomicInteger attemptCount = new AtomicInteger();
        private final EmailDeliveryStatus emailDeliveryStatus;
        private final int failuresBeforeSuccess;

        private CountingResendEmailService(ResendProperties resendProperties,
                                           EmailDeliveryStatus emailDeliveryStatus,
                                           int failuresBeforeSuccess) {
            super(resendProperties, emailDeliveryStatus);
            this.emailDeliveryStatus = emailDeliveryStatus;
            this.failuresBeforeSuccess = failuresBeforeSuccess;
        }

        @Override
        void executeSendAttempt(String apiKey, Map<String, Object> emailPayload) {
            int currentAttempt = attemptCount.incrementAndGet();
            if (failuresBeforeSuccess < 0 || currentAttempt <= failuresBeforeSuccess) {
                throw new IllegalStateException("Falha simulada na tentativa " + currentAttempt);
            }
        }

        @Override
        void awaitBeforeRetry(int attempt) {
        }
    }

    @Test
    void shouldRetryThreeTimesAndMarkEmailServiceUnavailableWhenAllAttemptsFail() {
        CountingResendEmailService emailService = buildEmailService("chave-valida", ALWAYS_FAIL);

        assertThrows(EmailDeliveryException.class,
                () -> emailService.sendPasswordResetEmail(RECIPIENT_EMAIL, "Ana Silva", "http://localhost/reset"));

        assertEquals(3, emailService.attemptCount.get());
        assertTrue(emailService.emailDeliveryStatus.isUnavailable());
    }

    @Test
    void shouldStopRetryingAfterSuccessfulAttempt() {
        CountingResendEmailService emailService = buildEmailService("chave-valida", 1);

        assertDoesNotThrow(
                () -> emailService.sendPasswordResetEmail(RECIPIENT_EMAIL, "Ana Silva", "http://localhost/reset"));

        assertEquals(2, emailService.attemptCount.get());
        assertFalse(emailService.emailDeliveryStatus.isUnavailable());
    }

    @Test
    void shouldSkipAttemptsWhenApiKeyIsMissing() {
        CountingResendEmailService emailService = buildEmailService("", ALWAYS_FAIL);

        assertDoesNotThrow(
                () -> emailService.sendPasswordResetEmail(RECIPIENT_EMAIL, "Ana Silva", "http://localhost/reset"));

        assertEquals(0, emailService.attemptCount.get());
        assertFalse(emailService.emailDeliveryStatus.isUnavailable());
    }

    private CountingResendEmailService buildEmailService(String apiKey, int failuresBeforeSuccess) {
        ResendProperties resendProperties = new ResendProperties();
        resendProperties.setKey(apiKey);
        resendProperties.setFromEmail("MAPA <onboarding@resend.dev>");

        EmailDeliveryStatus emailDeliveryStatus = new EmailDeliveryStatus(
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
        return new CountingResendEmailService(resendProperties, emailDeliveryStatus, failuresBeforeSuccess);
    }
}
