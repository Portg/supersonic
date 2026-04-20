package com.tencent.supersonic.headless.core.executor;

import com.tencent.supersonic.common.quota.TooManyRequestsException;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import com.tencent.supersonic.headless.core.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("JdbcExecutor")
@Slf4j
public class JdbcExecutor implements QueryExecutor {
    @Override
    public boolean accept(QueryStatement queryStatement) {
        return true;
    }

    @Override
    public SemanticQueryResp execute(QueryStatement queryStatement) {
        // accelerate query if possible
        for (QueryAccelerator queryAccelerator : ComponentFactory.getQueryAccelerators()) {
            if (queryAccelerator.check(queryStatement)) {
                SemanticQueryResp semanticQueryResp = queryAccelerator.query(queryStatement);
                if (Objects.nonNull(semanticQueryResp)
                        && !semanticQueryResp.getResultList().isEmpty()) {
                    log.info("query by Accelerator {}",
                            queryAccelerator.getClass().getSimpleName());
                    return semanticQueryResp;
                }
            }
        }

        SqlUtils sqlUtils = ContextUtils.getBean(SqlUtils.class);
        com.tencent.supersonic.common.metrics.Nl2sqlMetrics metrics =
                ContextUtils.getBean(com.tencent.supersonic.common.metrics.Nl2sqlMetrics.class);
        String sql = StringUtils.normalizeSpace(queryStatement.getSql());
        DatabaseResp database = queryStatement.getOntology().getDatabase();
        String dbType =
                (database != null && database.getType() != null) ? database.getType() : "unknown";
        String tenantId;
        try {
            Long tid = com.tencent.supersonic.common.context.TenantContext.getTenantId();
            tenantId = tid != null ? String.valueOf(tid) : null;
        } catch (Throwable ignore) {
            tenantId = null;
        }
        log.info("executing SQL, queryTraceId={}, sql={}",
                com.tencent.supersonic.common.metrics.QueryTraceContext.current().orElse("none"),
                sql);
        SemanticQueryResp queryResultWithColumns = new SemanticQueryResp();
        long startNanos = System.nanoTime();
        String outcome =
                com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants.OUTCOME_SUCCESS;
        long rowsReturned = -1;
        try {
            SqlUtils sqlUtil = sqlUtils.init(database);
            sqlUtil.queryInternal(queryStatement.getSql(), queryResultWithColumns);
            queryResultWithColumns.setSql(sql);
            rowsReturned = queryResultWithColumns.getResultList() == null ? 0
                    : queryResultWithColumns.getResultList().size();
        } catch (TooManyRequestsException e) {
            throw e;
        } catch (Exception e) {
            outcome = com.tencent.supersonic.common.metrics.Nl2sqlMetricConstants.OUTCOME_ERROR;
            log.error("queryInternal with error ", e);
            queryResultWithColumns.setErrorMsg(e.getMessage());
        } finally {
            java.time.Duration elapsed = java.time.Duration.ofNanos(System.nanoTime() - startNanos);
            if (metrics != null) {
                metrics.recordDb(dbType, elapsed, rowsReturned, outcome, tenantId);
            }
        }
        return queryResultWithColumns;
    }
}
