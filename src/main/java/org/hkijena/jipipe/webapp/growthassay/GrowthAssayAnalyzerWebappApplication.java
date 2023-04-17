package org.hkijena.jipipe.webapp.growthassay;

import org.hkijena.jipipe.webapp.growthassay.config.RuntimeConfig;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@EnableConfigurationProperties(RuntimeConfig.class)
public class GrowthAssayAnalyzerWebappApplication {

    @Autowired
    public GrowthAssayAnalyzerWebappApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(GrowthAssayAnalyzerWebappApplication.class, args);
    }
}
