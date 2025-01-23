package org.hkijena.jast.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.File;

@Configuration
public class EmbeddedPostgresConfig {

    @Value("${embedded-postgres.data-directory}")
    private String dataDirectory;

    @Value("${embedded-postgres.port}")
    private int port;

    @Value("${embedded-postgres.clean-on-start}")
    private boolean cleanOnStart;

    @Bean
    public DataSource dataSource() throws Exception {
        // Configure the EmbeddedPostgres instance
        EmbeddedPostgres.Builder postgresBuilder = EmbeddedPostgres.builder()
                .setDataDirectory(new File(dataDirectory).toPath())
                .setPort(port);

        if (cleanOnStart) {
            postgresBuilder.setCleanDataDirectory(true);
        }

        EmbeddedPostgres embeddedPostgres = postgresBuilder.start();

        // Return the DataSource for Spring Boot
        return embeddedPostgres.getPostgresDatabase();
    }
}
