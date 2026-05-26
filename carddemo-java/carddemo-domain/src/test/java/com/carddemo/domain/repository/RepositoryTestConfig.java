package com.carddemo.domain.repository;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.carddemo.domain.entity")
@EnableJpaRepositories(basePackages = "com.carddemo.domain.repository")
public class RepositoryTestConfig {
}
