package com.carddemo.migration;

import com.carddemo.migration.service.MigrationValidationService;
import com.carddemo.migration.service.VsamToPostgresqlMigrator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication(scanBasePackages = {"com.carddemo.migration", "com.carddemo.common"})
@EntityScan(basePackages = "com.carddemo.domain.entity")
@EnableJpaRepositories(basePackages = "com.carddemo.domain.repository")
public class CardDemoMigrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardDemoMigrationApplication.class, args);
    }

    @Bean
    public CommandLineRunner migrationRunner(VsamToPostgresqlMigrator migrator,
                                              MigrationValidationService validator,
                                              @org.springframework.beans.factory.annotation.Value("${migration.action:}") String action,
                                              @org.springframework.beans.factory.annotation.Value("${migration.dataDir:}") String dataDir) {
        return args -> {
            if ("migrate".equals(action)) {
                Path dir = dataDir.isEmpty() ? Paths.get("app/data/ASCII") : Paths.get(dataDir);
                var result = migrator.migrate(dir);
                System.out.println("Migration complete: " + result);
            }
            if ("validate".equals(action)) {
                Path dir = dataDir.isEmpty() ? Paths.get("app/data/ASCII") : Paths.get(dataDir);
                var result = validator.validate(dir);
                System.out.println("Validation complete: " + result);
            }
        };
    }
}
