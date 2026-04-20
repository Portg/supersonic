package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.metrics.Nl2sqlMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
@ConditionalOnBean(Nl2sqlMetrics.class)
@RequiredArgsConstructor
public class CorrectorMetricsConfiguration implements BeanPostProcessor {

    private final ObjectProvider<Nl2sqlMetrics> metricsProvider;

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) {
        if (bean instanceof SemanticCorrector && !(bean instanceof CorrectorMetricsDecorator)) {
            Nl2sqlMetrics metrics = metricsProvider.getIfAvailable();
            if (metrics == null) {
                return bean;
            }
            return new CorrectorMetricsDecorator((SemanticCorrector) bean,
                    bean.getClass().getSimpleName(), metrics);
        }
        return bean;
    }
}
