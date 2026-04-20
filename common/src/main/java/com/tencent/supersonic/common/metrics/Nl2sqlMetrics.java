package com.tencent.supersonic.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class Nl2sqlMetrics {

    private final MeterRegistry registry;
    private final TenantTagNormalizer tenantTagNormalizer;

    @Autowired
    public Nl2sqlMetrics(ObjectProvider<MeterRegistry> registryProvider,
            TenantTagNormalizer tenantTagNormalizer) {
        this(registryProvider.getIfAvailable(), tenantTagNormalizer);
    }

    public Nl2sqlMetrics(MeterRegistry registry, TenantTagNormalizer tenantTagNormalizer) {
        this.registry = registry;
        this.tenantTagNormalizer = tenantTagNormalizer;
    }

    public void recordStage(String stage, Duration duration, String outcome, String tenantId,
            String agentId, String parserName) {
        if (registry == null) {
            return;
        }
        Timer.builder(Nl2sqlMetricConstants.STAGE_DURATION)
                .tags(baseTags(stage, outcome, tenantId, agentId, parserName)).register(registry)
                .record(duration);
        Counter.builder(Nl2sqlMetricConstants.STAGE_OUTCOME_TOTAL)
                .tags(baseTags(stage, outcome, tenantId, agentId, parserName)).register(registry)
                .increment();
    }

    public StageTimer startStage(String stage, String tenantId, String agentId, String parserName) {
        return new StageTimer(this, stage, tenantId, agentId, parserName);
    }

    public void recordLlmLatency(String model, Duration duration, String outcome, String tenantId,
            String agentId) {
        if (registry == null) {
            return;
        }
        Timer.builder(Nl2sqlMetricConstants.LLM_DURATION)
                .tags(Tags.of(Nl2sqlMetricConstants.TagKeys.MODEL, safe(model),
                        Nl2sqlMetricConstants.TagKeys.OUTCOME, safe(outcome),
                        Nl2sqlMetricConstants.TagKeys.TENANT,
                        tenantTagNormalizer.normalize(tenantId),
                        Nl2sqlMetricConstants.TagKeys.AGENT, safe(agentId),
                        Nl2sqlMetricConstants.TagKeys.MODULE, Nl2sqlMetricConstants.MODULE))
                .register(registry).record(duration);
    }

    public void recordLlmTokens(String model, long promptTokens, long completionTokens) {
        if (registry == null) {
            return;
        }
        if (promptTokens > 0) {
            Counter.builder(Nl2sqlMetricConstants.LLM_TOKENS_TOTAL)
                    .tags(Tags.of(Nl2sqlMetricConstants.TagKeys.MODEL, safe(model),
                            Nl2sqlMetricConstants.TagKeys.KIND, "prompt",
                            Nl2sqlMetricConstants.TagKeys.MODULE, Nl2sqlMetricConstants.MODULE))
                    .register(registry).increment(promptTokens);
        }
        if (completionTokens > 0) {
            Counter.builder(Nl2sqlMetricConstants.LLM_TOKENS_TOTAL)
                    .tags(Tags.of(Nl2sqlMetricConstants.TagKeys.MODEL, safe(model),
                            Nl2sqlMetricConstants.TagKeys.KIND, "completion",
                            Nl2sqlMetricConstants.TagKeys.MODULE, Nl2sqlMetricConstants.MODULE))
                    .register(registry).increment(completionTokens);
        }
    }

    public void recordMapperHit(String mapperName, boolean hit, String tenantId) {
        if (registry == null) {
            return;
        }
        Counter.builder(Nl2sqlMetricConstants.MAPPER_HITS_TOTAL)
                .tags(Tags.of(Nl2sqlMetricConstants.TagKeys.MAPPER, safe(mapperName),
                        Nl2sqlMetricConstants.TagKeys.HIT, String.valueOf(hit),
                        Nl2sqlMetricConstants.TagKeys.TENANT,
                        tenantTagNormalizer.normalize(tenantId),
                        Nl2sqlMetricConstants.TagKeys.MODULE, Nl2sqlMetricConstants.MODULE))
                .register(registry).increment();
    }

    public void recordDb(String dbType, Duration duration, long rowsReturned, String outcome,
            String tenantId) {
        if (registry == null) {
            return;
        }
        Timer.builder(Nl2sqlMetricConstants.DB_DURATION)
                .tags(Tags.of(Nl2sqlMetricConstants.TagKeys.DB_TYPE, safe(dbType),
                        Nl2sqlMetricConstants.TagKeys.OUTCOME, safe(outcome),
                        Nl2sqlMetricConstants.TagKeys.TENANT,
                        tenantTagNormalizer.normalize(tenantId),
                        Nl2sqlMetricConstants.TagKeys.MODULE, Nl2sqlMetricConstants.MODULE))
                .register(registry).record(duration);
        if (rowsReturned >= 0) {
            DistributionSummary.builder(Nl2sqlMetricConstants.DB_ROWS_RETURNED)
                    .tags(Tags.of(Nl2sqlMetricConstants.TagKeys.DB_TYPE, safe(dbType),
                            Nl2sqlMetricConstants.TagKeys.MODULE, Nl2sqlMetricConstants.MODULE))
                    .register(registry).record(rowsReturned);
        }
    }

    Tags baseTags(String stage, String outcome, String tenantId, String agentId,
            String parserName) {
        return Tags.of(Nl2sqlMetricConstants.TagKeys.STAGE, safe(stage),
                Nl2sqlMetricConstants.TagKeys.OUTCOME, safe(outcome),
                Nl2sqlMetricConstants.TagKeys.TENANT, tenantTagNormalizer.normalize(tenantId),
                Nl2sqlMetricConstants.TagKeys.AGENT, safe(agentId),
                Nl2sqlMetricConstants.TagKeys.PARSER, safe(parserName),
                Nl2sqlMetricConstants.TagKeys.MODULE, Nl2sqlMetricConstants.MODULE);
    }

    private static String safe(String v) {
        return (v == null || v.isBlank()) ? "unknown" : v;
    }

    public static final class StageTimer implements AutoCloseable {

        private final Nl2sqlMetrics owner;
        private final String stage;
        private final String tenantId;
        private final String agentId;
        private final String parserName;
        private final long startNanos = System.nanoTime();
        private String outcome = Nl2sqlMetricConstants.OUTCOME_SUCCESS;
        private String mapperName;
        private String correctorName;
        private boolean stopped;

        StageTimer(Nl2sqlMetrics owner, String stage, String tenantId, String agentId,
                String parserName) {
            this.owner = owner;
            this.stage = stage;
            this.tenantId = tenantId;
            this.agentId = agentId;
            this.parserName = parserName;
        }

        public StageTimer markMapper(String mapperName) {
            this.mapperName = mapperName;
            return this;
        }

        public StageTimer markCorrector(String correctorName) {
            this.correctorName = correctorName;
            return this;
        }

        public void failed(String outcome) {
            this.outcome = (outcome == null) ? Nl2sqlMetricConstants.OUTCOME_ERROR : outcome;
        }

        public void timedOut() {
            this.outcome = Nl2sqlMetricConstants.OUTCOME_TIMEOUT;
        }

        @Override
        public void close() {
            if (stopped) {
                return;
            }
            stopped = true;
            if (owner.registry == null) {
                return;
            }
            Duration d = Duration.ofNanos(System.nanoTime() - startNanos);
            Tags tags = owner.baseTags(stage, outcome, tenantId, agentId, parserName);
            if (mapperName != null) {
                tags = tags.and(Nl2sqlMetricConstants.TagKeys.MAPPER, mapperName);
            }
            if (correctorName != null) {
                tags = tags.and(Nl2sqlMetricConstants.TagKeys.CORRECTOR, correctorName);
            }
            Timer.builder(Nl2sqlMetricConstants.STAGE_DURATION).tags(tags).register(owner.registry)
                    .record(d.toNanos(), TimeUnit.NANOSECONDS);
            Counter.builder(Nl2sqlMetricConstants.STAGE_OUTCOME_TOTAL).tags(tags)
                    .register(owner.registry).increment();
        }
    }
}
