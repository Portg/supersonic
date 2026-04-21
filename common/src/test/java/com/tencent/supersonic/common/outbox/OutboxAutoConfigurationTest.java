package com.tencent.supersonic.common.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class));

    @Test
    @DisplayName("默认配置下 OutboxProperties bean 已注册且 enabled=true")
    void registersOutboxPropertiesBean() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(OutboxProperties.class);
            assertThat(ctx.getBean(OutboxProperties.class).isEnabled()).isTrue();
        });
    }

    @Test
    @DisplayName("自定义属性值可正确绑定到 OutboxProperties")
    void bindsConfiguredProperties() {
        runner.withPropertyValues("s2.outbox.enabled=false", "s2.outbox.batch-size=32",
                "s2.outbox.max-attempts=7").run(ctx -> {
                    OutboxProperties props = ctx.getBean(OutboxProperties.class);
                    assertThat(props.isEnabled()).isFalse();
                    assertThat(props.getBatchSize()).isEqualTo(32);
                    assertThat(props.getMaxAttempts()).isEqualTo(7);
                });
    }

    @Test
    @DisplayName("OutboxAutoConfiguration 已在 AutoConfiguration.imports 文件中注册")
    void isRegisteredForSpringBootAutoConfigurationImport() {
        assertThat(ImportCandidates.load(AutoConfiguration.class,
                Thread.currentThread().getContextClassLoader()))
                        .contains(OutboxAutoConfiguration.class.getName());
    }
}
