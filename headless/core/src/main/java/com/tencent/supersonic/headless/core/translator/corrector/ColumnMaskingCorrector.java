package com.tencent.supersonic.headless.core.translator.corrector;

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
import java.util.stream.Collectors;

@Slf4j
public class ColumnMaskingCorrector implements PhysicalSqlCorrector {

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

        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (!(stmt instanceof Select select))
                return sql;
            boolean[] modified = {false};
            walk(select, colToMask, modified);
            return modified[0] ? stmt.toString() : sql;
        } catch (JSQLParserException e) {
            log.warn("ColumnMaskingCorrector parse failed; returning unchanged. err={}",
                    e.getMessage());
            return sql;
        }
    }

    private void walk(Select select, Map<String, String> colToMask, boolean[] modified) {
        if (select.getWithItemsList() != null) {
            for (WithItem w : select.getWithItemsList()) {
                if (w.getSelect() != null)
                    walk(w.getSelect(), colToMask, modified);
            }
        }
        if (select instanceof PlainSelect ps) {
            rewritePlain(ps, colToMask, modified);
        } else if (select instanceof SetOperationList sol) {
            if (sol.getSelects() != null) {
                for (Select child : sol.getSelects())
                    walk(child, colToMask, modified);
            }
        } else if (select instanceof ParenthesedSelect pss) {
            walk(pss.getSelect(), colToMask, modified);
        }
    }

    private void rewritePlain(PlainSelect ps, Map<String, String> colToMask, boolean[] modified) {
        List<SelectItem<?>> items = ps.getSelectItems();
        if (items == null)
            return;
        for (int i = 0; i < items.size(); i++) {
            SelectItem<?> si = items.get(i);
            Expression expr = si.getExpression();
            // SELECT * cannot be surgically masked without schema — skip
            if (expr instanceof AllColumns || expr instanceof AllTableColumns)
                continue;
            if (!(expr instanceof Column col))
                continue;
            String name = col.getColumnName();
            if (name == null)
                continue;
            String mask = colToMask.get(name.toLowerCase(Locale.ROOT));
            if (mask == null)
                continue;
            String rendered = String.format(mask, col.toString());
            try {
                Expression newExpr = CCJSqlParserUtil.parseExpression(rendered);
                Alias existingAlias = si.getAlias();
                Alias alias = existingAlias != null ? existingAlias : new Alias(name);
                SelectItem<Expression> replaced = new SelectItem<>(newExpr);
                replaced.setAlias(alias);
                items.set(i, replaced);
                modified[0] = true;
            } catch (JSQLParserException e) {
                log.warn("Failed to render mask template '{}' for column '{}'", mask, name);
            }
        }
    }
}
