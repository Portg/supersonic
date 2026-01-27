package com.tencent.supersonic.common.config;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Database configuration that supports multiple profiles (h2, mysql, postgres). Properties are
 * loaded from the active profile's application-{profile}.yaml file.
 */
@Slf4j
@Configuration
public class DataBaseConfig {

    @Value("${spring.datasource.url:jdbc:h2:mem:semantic;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_UPPER=false;QUERY_TIMEOUT=30}")
    private String url;

    @Value("${spring.datasource.username:root}")
    private String username;

    @Value("${spring.datasource.password:semantic}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.h2.Driver}")
    private String driverClassName;

    @Value("${spring.datasource.druid.initial-size:5}")
    private int initialSize;

    @Value("${spring.datasource.druid.min-idle:5}")
    private int minIdle;

    @Value("${spring.datasource.druid.max-active:20}")
    private int maxActive;

    @Value("${spring.datasource.druid.max-wait:60000}")
    private long maxWait;

    @Bean
    @Primary
    public DataSource dataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);

        // Connection pool settings
        dataSource.setInitialSize(initialSize);
        dataSource.setMinIdle(minIdle);
        dataSource.setMaxActive(maxActive);
        dataSource.setMaxWait(maxWait);

        // Connection validation
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        dataSource.setValidationQuery(getValidationQuery());

        // Connection lifecycle
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(300000);

        log.info("DataSource initialized: url={}, driver={}", url, driverClassName);
        return dataSource;
    }

    /**
     * Get validation query based on database type.
     */
    private String getValidationQuery() {
        if (driverClassName != null) {
            if (driverClassName.contains("mysql") || driverClassName.contains("h2")) {
                return "SELECT 1";
            } else if (driverClassName.contains("postgresql")) {
                return "SELECT 1";
            } else if (driverClassName.contains("oracle")) {
                return "SELECT 1 FROM DUAL";
            }
        }
        return "SELECT 1";
    }
}
