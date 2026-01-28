package com.tencent.supersonic.common.config;

import javax.sql.DataSource;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.github.pagehelper.PageInterceptor;
import com.tencent.supersonic.common.mybatis.TenantSqlInterceptor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Configuration
@MapperScan(value = "com.tencent.supersonic", annotationClass = Mapper.class)
@EnableConfigurationProperties(TenantConfig.class)
public class MybatisConfig {

    private static final String MAPPER_LOCATION = "classpath*:mapper/**/*.xml";

    private final TenantConfig tenantConfig;

    public MybatisConfig(TenantConfig tenantConfig) {
        this.tenantConfig = tenantConfig;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        bean.setConfiguration(configuration);
        bean.setDataSource(dataSource);

        List<Interceptor> interceptors = new ArrayList<>();

        // Always register PageHelper interceptor for pagination support
        PageInterceptor pageInterceptor = new PageInterceptor();
        Properties pageProperties = new Properties();
        pageProperties.setProperty("reasonable", "true");
        pageProperties.setProperty("supportMethodsArguments", "true");
        pageInterceptor.setProperties(pageProperties);
        interceptors.add(pageInterceptor);

        // Conditionally register tenant SQL interceptor for multi-tenancy
        if (tenantConfig.isEnabled()) {
            interceptors.add(new TenantSqlInterceptor(tenantConfig));
        }

        bean.setPlugins(interceptors.toArray(new Interceptor[0]));
        bean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources(MAPPER_LOCATION));
        return bean.getObject();
    }
}
