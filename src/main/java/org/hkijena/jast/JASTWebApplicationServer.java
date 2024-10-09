package org.hkijena.jast;

import org.hkijena.jast.config.AccountConfig;
import org.hkijena.jast.config.RuntimeParametersConfig;
import org.hkijena.jast.config.RuntimeConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({RuntimeConfig.class, AccountConfig.class, RuntimeParametersConfig.class})
public class JASTWebApplicationServer {

    public JASTWebApplicationServer() {
    }

    public static void main(String[] args) {
        SpringApplication.run(JASTWebApplicationServer.class, args);
    }
}
