package org.hkijena.jipipe.webapp.growthassay;

import org.hkijena.jipipe.webapp.growthassay.config.AccountConfig;
import org.hkijena.jipipe.webapp.growthassay.config.RuntimeParametersConfig;
import org.hkijena.jipipe.webapp.growthassay.config.RuntimeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({RuntimeConfig.class, AccountConfig.class, RuntimeParametersConfig.class})
public class WebApplicationServer {

    public WebApplicationServer() {
    }

    public static void main(String[] args) {
        SpringApplication.run(WebApplicationServer.class, args);
    }
}
