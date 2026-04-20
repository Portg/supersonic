package com.tencent.supersonic.feishu.server.metrics;

import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * 飞书模块指标切面：对消息处理器耗时与消息发送次数进行埋点。
 *
 * <p>
 * 所有 meter 创建/操作委托给 {@link FeishuMeterBinder}，确保 tag 和 registry 管理统一。 仅在存在
 * {@link FeishuMeterBinder} 时生效。
 * </p>
 */
@Aspect
@Component
@ConditionalOnBean(FeishuMeterBinder.class)
public class FeishuMetricsAspect {

    private final FeishuMeterBinder meterBinder;

    public FeishuMetricsAspect(FeishuMeterBinder meterBinder) {
        this.meterBinder = meterBinder;
    }

    @Around("execution(* com.tencent.supersonic.feishu.server.handler.MessageHandler+.handle(..))")
    public Object aroundHandle(ProceedingJoinPoint pjp) throws Throwable {
        Timer.Sample sample = meterBinder.startSample();
        String handler = pjp.getTarget().getClass().getSimpleName();
        try {
            Object result = pjp.proceed();
            meterBinder.stopQueryTimer(sample, handler, "success");
            return result;
        } catch (Throwable t) {
            meterBinder.stopQueryTimer(sample, handler, "error");
            throw t;
        }
    }

    @AfterReturning("execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.reply*(..)) || "
            + "execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.send*(..)) || "
            + "execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.uploadFile(..))")
    public void afterMessageSent(JoinPoint jp) {
        meterBinder.incrementMessageSent(extractType(jp));
    }

    @AfterThrowing("execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.reply*(..)) || "
            + "execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.send*(..)) || "
            + "execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.uploadFile(..))")
    public void afterMessageError(JoinPoint jp) {
        meterBinder.incrementMessageSendError(extractType(jp));
    }

    private String extractType(JoinPoint jp) {
        String method = jp.getSignature().getName();
        if (method.contains("Text")) {
            return "text";
        }
        if (method.contains("Card")) {
            return "card";
        }
        return "file";
    }
}
