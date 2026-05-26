package com.carddemo.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.carddemo")
@EntityScan(basePackages = "com.carddemo.domain.entity")
@EnableJpaRepositories(basePackages = "com.carddemo.domain.repository")
public class CardDemoBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardDemoBatchApplication.class, args);
    }
}
