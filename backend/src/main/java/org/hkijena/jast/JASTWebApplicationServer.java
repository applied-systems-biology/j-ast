package org.hkijena.jast;

import org.hkijena.jast.config.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({RuntimeConfig.class, AccountConfig.class, JwtConfig.class, ProviderConfig.class, PresetsConfig.class})
public class JASTWebApplicationServer {

    public JASTWebApplicationServer() {
    }

    public static void main(String[] args) {
        SpringApplication.run(JASTWebApplicationServer.class, args);
    }
}
