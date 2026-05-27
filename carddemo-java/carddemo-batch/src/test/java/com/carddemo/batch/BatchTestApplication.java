package com.carddemo.batch;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(
        basePackages = {"com.carddemo.batch", "com.carddemo.domain", "com.carddemo.common"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = CardDemoBatchApplication.class
        )
)
@EntityScan(basePackages = "com.carddemo.domain.entity")
@EnableJpaRepositories(basePackages = "com.carddemo.domain.repository")
public class BatchTestApplication {
}
