package com.tencent.supersonic.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.tencent.supersonic.common.outbox")
@MapperScan(basePackages = "com.tencent.supersonic.common.outbox")
@EnableConfigurationProperties(OutboxProperties.class)
@EnableScheduling
class OutboxTestConfig {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    public static void main(String[] args) {
        SpringApplication.run(OutboxTestConfig.class, args);
    }
}
