package com.tencent.supersonic.headless.core.translator.corrector;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.core.translator.corrector.policy.RowPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RowLevelPolicyCorrectorTest {

    private String norm(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private PolicyContext ctx(RowPolicy... policies) {
        PolicyContext c = new PolicyContext();
        c.setUser(User.get(0L, "alice"));
        c.setRowPolicies(List.of(policies));
        return c;
    }

    @Test
    void noPolicies_passthrough() {
        String input = "SELECT a FROM t WHERE a = 1";
        String out = new RowLevelPolicyCorrector().rewrite(input, ctx());
        assertEquals(norm(input), norm(out));
    }

    @Test
    void simpleWhereInjection() {
        RowPolicy p = new RowPolicy("P", 1L, List.of("t"), "region = 'APAC'", null);
        String out = new RowLevelPolicyCorrector().rewrite("SELECT a FROM t WHERE a = 1", ctx(p));
        assertTrue(norm(out).contains("AND (region = 'APAC')"));
    }

    @Test
    void injectsIntoBothUnionBranches() {
        RowPolicy p = new RowPolicy("P", 1L, List.of("t"), "region = 'APAC'", null);
        String out = new RowLevelPolicyCorrector()
                .rewrite("SELECT a FROM t WHERE a=1 UNION ALL SELECT a FROM t WHERE a=2", ctx(p));
        long occurrences = norm(out).split("region = 'APAC'", -1).length - 1;
        assertEquals(2, occurrences);
    }

    @Test
    void injectsIntoSubSelectInFrom() {
        RowPolicy p = new RowPolicy("P", 1L, List.of("t"), "r = 1", null);
        String out = new RowLevelPolicyCorrector()
                .rewrite("SELECT x.a FROM (SELECT a FROM t WHERE a=1) x", ctx(p));
        assertTrue(norm(out).contains("AND (r = 1)"));
    }

    @Test
    void injectsIntoCte() {
        RowPolicy p = new RowPolicy("P", 1L, List.of("t"), "r = 1", null);
        String out = new RowLevelPolicyCorrector()
                .rewrite("WITH c AS (SELECT a FROM t WHERE a=1) SELECT * FROM c", ctx(p));
        assertTrue(norm(out).contains("AND (r = 1)"));
    }

    @Test
    void onlyAppliesToReferencedTables() {
        RowPolicy p = new RowPolicy("P", 1L, List.of("orders"), "region = 'APAC'", null);
        String out =
                new RowLevelPolicyCorrector().rewrite("SELECT a FROM products WHERE a=1", ctx(p));
        assertEquals(norm("SELECT a FROM products WHERE a=1"), norm(out));
    }

    @Test
    void skipsRewriteWhenShadowModeEnabled() {
        RowPolicy p = new RowPolicy("P", 1L, List.of("t"), "r = 1", null);
        PolicyContext c = ctx(p);
        c.setShadowMode(true);
        String input = "SELECT a FROM t";
        String out = new RowLevelPolicyCorrector().rewrite(input, c);
        assertEquals(norm(input), norm(out));
    }

    @Test
    void malformedSqlReturnsOriginalUnchanged() {
        RowPolicy p = new RowPolicy("P", 1L, List.of("t"), "r = 1", null);
        String broken = "SELECT FROM WHERE";
        String out = new RowLevelPolicyCorrector().rewrite(broken, ctx(p));
        assertEquals(broken, out);
    }
}
