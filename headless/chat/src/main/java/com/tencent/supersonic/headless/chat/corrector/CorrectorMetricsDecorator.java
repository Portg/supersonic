package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants;
import com.tencent.supersonic.common.metrics.Nl2sqlMetrics;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;

public class CorrectorMetricsDecorator implements SemanticCorrector {

    private final SemanticCorrector delegate;
    private final String correctorName;
    private final Nl2sqlMetrics metrics;

    public CorrectorMetricsDecorator(SemanticCorrector delegate, String correctorName,
            Nl2sqlMetrics metrics) {
        this.delegate = delegate;
        this.correctorName = correctorName;
        this.metrics = metrics;
    }

    @Override
    public void correct(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo) {
        String tenantId;
        try {
            Long tid = com.tencent.supersonic.common.context.TenantContext.getTenantId();
            tenantId = tid != null ? String.valueOf(tid) : null;
        } catch (Throwable ignore) {
            tenantId = null;
        }
        try (Nl2sqlMetrics.StageTimer t =
                metrics.startStage("corrector", tenantId, "unknown", "NL2SQLParser")
                        .markCorrector(correctorName)) {
            try {
                delegate.correct(chatQueryContext, semanticParseInfo);
            } catch (RuntimeException e) {
                t.failed(Nl2sqlMetricConstants.OUTCOME_ERROR);
                throw e;
            }
        }
    }

    public SemanticCorrector delegate() {
        return delegate;
    }
}
