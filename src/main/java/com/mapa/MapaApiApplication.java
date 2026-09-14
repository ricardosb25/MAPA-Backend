package com.mapa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MapaApiApplication {

    public static void main(String[] applicationArguments) {
        SpringApplication.run(MapaApiApplication.class, applicationArguments);
    }

}

