package com.tencent.supersonic.headless.core.translator.corrector;

import com.tencent.supersonic.headless.core.translator.corrector.audit.PolicyAuditEntry;
import com.tencent.supersonic.headless.core.translator.corrector.audit.PolicyAuditLogger;
import com.tencent.supersonic.headless.core.translator.corrector.policy.RowPolicy;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.WithItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
public class RowLevelPolicyCorrector implements PhysicalSqlCorrector {

    private final PolicyAuditLogger auditLogger = new PolicyAuditLogger();

    @Override
    public String rewrite(String sql, PolicyContext ctx) {
        if (sql == null || sql.isBlank())
            return sql;
        if (ctx == null || ctx.isShadowMode())
            return sql;
        if (ctx.getRowPolicies().isEmpty())
            return sql;

        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (stmt instanceof Select select) {
                boolean[] modified = {false};
                walk(select, ctx, modified);
                return modified[0] ? stmt.toString() : sql;
            }
            return sql;
        } catch (JSQLParserException e) {
            log.warn("RowLevelPolicyCorrector parse failed; returning SQL unchanged. err={}",
                    e.getMessage());
            return sql;
        }
    }

    private void walk(Select select, PolicyContext ctx, boolean[] modified) {
        if (select.getWithItemsList() != null) {
            for (WithItem w : select.getWithItemsList()) {
                if (w.getSelect() != null)
                    walk(w.getSelect(), ctx, modified);
            }
        }
        if (select instanceof PlainSelect ps) {
            walkPlain(ps, ctx, modified);
        } else if (select instanceof SetOperationList sol) {
            if (sol.getSelects() != null) {
                for (Select child : sol.getSelects())
                    walk(child, ctx, modified);
            }
        } else if (select instanceof ParenthesedSelect pss) {
            walk(pss.getSelect(), ctx, modified);
        }
    }

    private void walkPlain(PlainSelect ps, PolicyContext ctx, boolean[] modified) {
        if (ps.getFromItem()instanceof ParenthesedSelect pss)
            walk(pss.getSelect(), ctx, modified);
        if (ps.getJoins() != null) {
            for (Join j : ps.getJoins()) {
                if (j.getFromItem()instanceof ParenthesedSelect pss)
                    walk(pss.getSelect(), ctx, modified);
            }
        }

        List<String> refs = referencedTableNames(ps).stream().map(t -> t.toLowerCase(Locale.ROOT))
                .distinct().toList();

        for (RowPolicy p : ctx.getRowPolicies()) {
            if (p == null || p.getTableBizNames() == null || p.getFilterExpression() == null)
                continue;
            boolean match = p.getTableBizNames().stream().map(t -> t.toLowerCase(Locale.ROOT))
                    .anyMatch(refs::contains);
            if (!match)
                continue;
            Expression cond = parseCondSafe(p.getFilterExpression());
            if (cond == null)
                continue;
            Expression wrapped = new Parenthesis(cond);
            if (ps.getWhere() == null) {
                ps.setWhere(wrapped);
            } else {
                ps.setWhere(new AndExpression(ps.getWhere(), wrapped));
            }
            modified[0] = true;
            String userName = ctx.getUser() != null ? ctx.getUser().getName() : "unknown";
            auditLogger.log(new PolicyAuditEntry(p.getPolicyId(), userName, "row", null, null,
                    PolicyAuditLogger.digest(ps.toString())));
        }
    }

    private List<String> referencedTableNames(PlainSelect ps) {
        List<String> names = new ArrayList<>();
        if (ps.getFromItem()instanceof Table t)
            names.add(t.getName());
        if (ps.getJoins() != null) {
            for (Join j : ps.getJoins()) {
                if (j.getFromItem()instanceof Table t)
                    names.add(t.getName());
            }
        }
        return names;
    }

    private Expression parseCondSafe(String expr) {
        try {
            return CCJSqlParserUtil.parseCondExpression(expr);
        } catch (JSQLParserException e) {
            log.warn("Failed to parse policy expression '{}' — skipping", expr);
            return null;
        }
    }
}
