package com.mapa.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.frontend")
public class FrontendProperties {

    private String baseUrl = "http://localhost:4200";

}