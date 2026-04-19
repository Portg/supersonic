package com.tencent.supersonic.headless.core.translator.corrector;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.core.translator.corrector.policy.ColumnPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnMaskingCorrectorTest {

    private String norm(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    private PolicyContext ctx(ColumnPolicy... cps) {
        PolicyContext c = new PolicyContext();
        c.setUser(User.get(0L, "alice"));
        c.setColumnPolicies(List.of(cps));
        return c;
    }

    @Test
    void noPolicies_passthrough() {
        String in = "SELECT user_id, phone FROM t";
        assertEquals(norm(in), norm(new ColumnMaskingCorrector().rewrite(in, ctx())));
    }

    @Test
    void wrapsPlainColumn() {
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        String out = new ColumnMaskingCorrector().rewrite("SELECT user_id, phone FROM t", ctx(cp));
        assertTrue(norm(out).contains("CONCAT(LEFT(phone, 3), '****') AS phone"));
    }

    @Test
    void preservesExistingAliasByReplacingWithMaskedAlias() {
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        String out =
                new ColumnMaskingCorrector().rewrite("SELECT user_id, phone AS p FROM t", ctx(cp));
        assertTrue(norm(out).contains("CONCAT(LEFT(phone, 3), '****') AS p"));
    }

    @Test
    void doesNotWrapNonMaskedColumns() {
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        String out = new ColumnMaskingCorrector().rewrite("SELECT user_id FROM t", ctx(cp));
        assertEquals(norm("SELECT user_id FROM t"), norm(out));
    }

    @Test
    void shadowModeSkipsRewrite() {
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        PolicyContext c = ctx(cp);
        c.setShadowMode(true);
        String in = "SELECT phone FROM t";
        assertEquals(norm(in), norm(new ColumnMaskingCorrector().rewrite(in, c)));
    }

    @Test
    void selectStarFailsClosedWhenMaskingIsActive() {
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        assertThrows(IllegalStateException.class,
                () -> new ColumnMaskingCorrector().rewrite("SELECT * FROM t", ctx(cp)));
    }

    @Test
    void malformedSqlFailsClosed() {
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        String broken = "SELECT FROM WHERE";
        assertThrows(IllegalStateException.class,
                () -> new ColumnMaskingCorrector().rewrite(broken, ctx(cp)));
    }

    @Test
    void malformedMaskTemplateFailsClosed() {
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),%s)");
        assertThrows(IllegalStateException.class,
                () -> new ColumnMaskingCorrector().rewrite("SELECT phone FROM t", ctx(cp)));
    }

    @Test
    void multiModelMaskingFailsClosedWithoutScopedPolicies() {
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        PolicyContext c = ctx(cp);
        c.setModelIds(Set.of(1L, 2L));
        assertThrows(IllegalStateException.class,
                () -> new ColumnMaskingCorrector().rewrite("SELECT phone FROM t", c));
    }

    @Test
    void sensitiveAliasExpressionFailsClosed() {
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        assertThrows(IllegalStateException.class, () -> new ColumnMaskingCorrector()
                .rewrite("SELECT COALESCE(phone, '') AS phone FROM t", ctx(cp)));
    }

    @Test
    void unionAllMasksBothBranches() {
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        String out = new ColumnMaskingCorrector().rewrite(
                "SELECT user_id, phone FROM t1 UNION ALL SELECT user_id, phone FROM t2", ctx(cp));
        long count = norm(out).split("CONCAT", -1).length - 1;
        assertEquals(2, count, "phone should be masked in both UNION branches");
    }

    @Test
    void cteMasksColumnInsideCte() {
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        String out = new ColumnMaskingCorrector().rewrite(
                "WITH cte AS (SELECT user_id, phone FROM t) SELECT user_id, phone FROM cte",
                ctx(cp));
        assertTrue(norm(out).contains("CONCAT"), "phone inside CTE should be masked");
    }

    @Test
    void parenthesedSubqueryMasksColumn() {
        ColumnPolicy cp = new ColumnPolicy("C1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        String out = new ColumnMaskingCorrector()
                .rewrite("SELECT u.phone FROM (SELECT user_id, phone FROM t) u", ctx(cp));
        assertTrue(norm(out).contains("CONCAT"), "phone in subquery should be masked");
    }
}
