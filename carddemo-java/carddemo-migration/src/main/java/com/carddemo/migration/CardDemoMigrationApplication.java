package com.carddemo.migration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.carddemo")
@EntityScan(basePackages = "com.carddemo.domain.entity")
@EnableJpaRepositories(basePackages = "com.carddemo.domain.repository")
public class CardDemoMigrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardDemoMigrationApplication.class, args);
    }
}
