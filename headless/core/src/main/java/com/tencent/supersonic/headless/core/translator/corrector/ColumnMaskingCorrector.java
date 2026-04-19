package com.tencent.supersonic.headless.core.translator.corrector;

import com.tencent.supersonic.headless.core.translator.corrector.audit.PolicyAuditEntry;
import com.tencent.supersonic.headless.core.translator.corrector.audit.PolicyAuditLogger;
import com.tencent.supersonic.headless.core.translator.corrector.policy.ColumnPolicy;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.WithItem;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ColumnMaskingCorrector implements PhysicalSqlCorrector {

    private final PolicyAuditLogger auditLogger = new PolicyAuditLogger();

    @Override
    public String rewrite(String sql, PolicyContext ctx) {
        if (sql == null || sql.isBlank())
            return sql;
        if (ctx == null || ctx.isShadowMode())
            return sql;
        List<ColumnPolicy> policies = ctx.getColumnPolicies();
        if (policies.isEmpty())
            return sql;

        Map<String, String> colToMask = policies.stream().filter(
                p -> p != null && p.getColumnBizName() != null && p.getMaskTemplate() != null)
                .collect(Collectors.toMap(p -> p.getColumnBizName().toLowerCase(Locale.ROOT),
                        ColumnPolicy::getMaskTemplate, (a, b) -> a));

        if (colToMask.isEmpty())
            return sql;
        if (ctx.getModelIds() != null && ctx.getModelIds().size() > 1) {
            throw new IllegalStateException(
                    "column masking across multiple models requires scoped policies");
        }

        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (!(stmt instanceof Select select))
                return sql;
            boolean[] modified = {false};
            walk(select, colToMask, modified, ctx);
            return modified[0] ? stmt.toString() : sql;
        } catch (JSQLParserException e) {
            log.warn("ColumnMaskingCorrector parse failed. err={}", e.getMessage());
            throw new IllegalStateException("failed to parse SQL for column masking", e);
        }
    }

    private void walk(Select select, Map<String, String> colToMask, boolean[] modified,
            PolicyContext ctx) {
        if (select.getWithItemsList() != null) {
            for (WithItem w : select.getWithItemsList()) {
                if (w.getSelect() != null)
                    walk(w.getSelect(), colToMask, modified, ctx);
            }
        }
        if (select instanceof PlainSelect ps) {
            rewritePlain(ps, colToMask, modified, ctx);
        } else if (select instanceof SetOperationList sol) {
            if (sol.getSelects() != null) {
                for (Select child : sol.getSelects())
                    walk(child, colToMask, modified, ctx);
            }
        } else if (select instanceof ParenthesedSelect pss) {
            walk(pss.getSelect(), colToMask, modified, ctx);
        }
    }

    private void rewritePlain(PlainSelect ps, Map<String, String> colToMask, boolean[] modified,
            PolicyContext ctx) {
        List<SelectItem<?>> items = ps.getSelectItems();
        if (items == null)
            return;
        for (int i = 0; i < items.size(); i++) {
            SelectItem<?> si = items.get(i);
            Expression expr = si.getExpression();
            if (expr instanceof AllColumns || expr instanceof AllTableColumns) {
                throw new IllegalStateException(
                        "SELECT * is not allowed when column masking policies are active");
            }
            if (!(expr instanceof Column col)) {
                failIfSensitiveAlias(si, colToMask.keySet());
                continue;
            }
            String name = col.getColumnName();
            if (name == null)
                continue;
            String mask = colToMask.get(name.toLowerCase(Locale.ROOT));
            if (mask == null)
                continue;
            try {
                String rendered = String.format(mask, col.toString());
                Expression newExpr = CCJSqlParserUtil.parseExpression(rendered);
                Alias existingAlias = si.getAlias();
                Alias alias = existingAlias != null ? existingAlias : new Alias(name);
                SelectItem<Expression> replaced = new SelectItem<>(newExpr);
                replaced.setAlias(alias);
                items.set(i, replaced);
                modified[0] = true;
                if (ctx.isAuditLogEnabled()) {
                    String userName = ctx.getUser() != null ? ctx.getUser().getName() : "unknown";
                    auditLogger.log(new PolicyAuditEntry("col-" + name, userName, "column", null,
                            null, PolicyAuditLogger.digest(ps.toString())));
                }
            } catch (JSQLParserException e) {
                log.warn("Failed to parse mask template '{}' for column '{}'", mask, name);
                throw new IllegalStateException("failed to parse column mask expression", e);
            } catch (RuntimeException e) {
                log.warn("Failed to render mask template '{}' for column '{}'", mask, name);
                throw new IllegalStateException("failed to render column mask expression", e);
            }
        }
    }

    private void failIfSensitiveAlias(SelectItem<?> si, Set<String> maskedColumns) {
        if (si.getAlias() == null || si.getAlias().getName() == null) {
            return;
        }
        String alias = si.getAlias().getName().toLowerCase(Locale.ROOT);
        if (maskedColumns.contains(alias)) {
            throw new IllegalStateException(
                    "expression projection cannot safely mask sensitive column alias: " + alias);
        }
    }
}
