package com.mapa.service;

import com.mapa.config.properties.ResendProperties;
import com.mapa.exception.EmailDeliveryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResendEmailService {

    private static final String RESEND_EMAILS_URL = "https://api.resend.com/emails";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(20);
    private static final int MAX_SEND_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(1);

    private final ResendProperties resendProperties;
    private final EmailDeliveryStatus emailDeliveryStatus;

    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetUrl) {
        String apiKey = resendProperties.getKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("RESEND_API_KEY não configurada; e-mail de redefinição para {} não foi enviado", recipientEmail);
            return;
        }

        Map<String, Object> emailPayload = Map.of(
                "from", resendProperties.getFromEmail(),
                "to", recipientEmail,
                "subject", "Redefinição de senha — MAPA",
                "html", buildPasswordResetHtml(recipientName, resetUrl)
        );

        Exception lastFailure = null;
        for (int attempt = 1; attempt <= MAX_SEND_ATTEMPTS; attempt++) {
            try {
                executeSendAttempt(apiKey, emailPayload);
                log.info("E-mail de redefinição de senha enviado para {} (tentativa {}/{})",
                        recipientEmail, attempt, MAX_SEND_ATTEMPTS);
                return;
            } catch (Exception sendException) {
                lastFailure = sendException;
                log.warn("Falha {}/{} ao enviar e-mail de redefinição para {}: {}",
                        attempt, MAX_SEND_ATTEMPTS, recipientEmail, sendException.getMessage());
                if (attempt < MAX_SEND_ATTEMPTS) {
                    awaitBeforeRetry(attempt);
                }
            }
        }

        emailDeliveryStatus.markFailure();
        throw new EmailDeliveryException(
                "Falha ao enviar o e-mail de redefinição após " + MAX_SEND_ATTEMPTS + " tentativas", lastFailure);
    }

    void executeSendAttempt(String apiKey, Map<String, Object> emailPayload) {
        buildClient().post()
                .uri(RESEND_EMAILS_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(emailPayload)
                .retrieve()
                .toBodilessEntity();
    }

    void awaitBeforeRetry(int attempt) {
        try {
            Thread.sleep(RETRY_DELAY.multipliedBy(attempt).toMillis());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private RestClient buildClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private String buildPasswordResetHtml(String recipientName, String resetUrl) {
        String safeName = escapeHtml(recipientName);
        String safeResetUrl = escapeHtml(resetUrl);
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <body style="margin:0;padding:0;background-color:#e5e7eb;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="640" cellpadding="0" cellspacing="0" align="center" style="margin:0 auto;">
                    <tr>
                      <td style="padding:32px 16px;">
                        <table role="presentation" width="576" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:16px;padding:32px;">
                          <tr>
                            <td>
                              <h1 style="margin:0 0 16px 0;font-size:24px;color:#0f172a;">Redefinição de senha</h1>
                              <p style="margin:0 0 16px 0;font-size:15px;color:#334155;line-height:1.5;">Olá, %s.</p>
                              <p style="margin:0 0 24px 0;font-size:15px;color:#334155;line-height:1.5;">Recebemos uma solicitação para redefinir a senha da sua conta no MAPA. Use o botão abaixo no prazo de 30 minutos:</p>
                              <a href="%s" style="display:inline-block;background-color:#0099ff;color:#ffffff;text-decoration:none;font-weight:700;font-size:15px;padding:14px 28px;border-radius:12px;">Redefinir senha</a>
                              <p style="margin:24px 0 0 0;font-size:13px;color:#64748b;line-height:1.5;">Se você não solicitou esta alteração, ignore este e-mail. Sua senha permanecerá a mesma.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(safeName, safeResetUrl);
    }

    private String escapeHtml(String rawValue) {
        if (rawValue == null) {
            return "";
        }
        return rawValue
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}