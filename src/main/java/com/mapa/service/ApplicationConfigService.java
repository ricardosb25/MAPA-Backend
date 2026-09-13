package com.mapa.service;

import com.mapa.config.properties.ApplicationProperties;
import com.mapa.config.properties.JwtProperties;
import com.mapa.config.properties.ResendProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Getter
@RequiredArgsConstructor
public class ApplicationConfigService {

    private final ResendProperties resendProperties;
    private final JwtProperties jwtProperties;
    private final ApplicationProperties applicationProperties;

    public String getResendApiKey() {
        return resendProperties.getKey();
    }

    public String getJwtSecret() {
        return jwtProperties.getSecret();
    }

    public String getApplicationName() {
        return applicationProperties.getName();
    }
}
