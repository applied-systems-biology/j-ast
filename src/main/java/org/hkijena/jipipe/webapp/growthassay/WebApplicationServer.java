package org.hkijena.jipipe.webapp.growthassay;

import org.hkijena.jipipe.webapp.growthassay.config.RuntimeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RuntimeConfig.class)
public class WebApplicationServer {

    @Autowired
    public WebApplicationServer() {
    }

    public static void main(String[] args) {
        SpringApplication.run(WebApplicationServer.class, args);
    }
}
