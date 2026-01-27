package com.tencent.supersonic.db;

import javax.sql.DataSource;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.common.mybatis.TenantSqlInterceptor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@MapperScan(value = "com.tencent.supersonic", annotationClass = Mapper.class)
public class MybatisConfig {

    private static final String MAPPER_LOCATION = "classpath*:mapper/**/*.xml";

    @Autowired(required = false)
    private TenantConfig tenantConfig;

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        bean.setConfiguration(configuration);
        bean.setDataSource(dataSource);

        // Register tenant SQL interceptor for multi-tenancy support (optional)
        if (tenantConfig != null) {
            TenantSqlInterceptor tenantSqlInterceptor = new TenantSqlInterceptor(tenantConfig);
            bean.setPlugins(new Interceptor[] {tenantSqlInterceptor});
        }

        bean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources(MAPPER_LOCATION));
        return bean.getObject();
    }
}
