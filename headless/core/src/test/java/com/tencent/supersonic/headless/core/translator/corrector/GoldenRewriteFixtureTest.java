package com.tencent.supersonic.headless.core.translator.corrector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.core.translator.corrector.policy.ColumnPolicy;
import com.tencent.supersonic.headless.core.translator.corrector.policy.RowPolicy;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoldenRewriteFixtureTest {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Case {
        public String name;
        public String user;
        public List<RowPolicy> rowPolicies;
        public List<ColumnPolicy> columnPolicies;
        public String inputSql;
        public String expectedSql;

        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Case> loadCases() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = GoldenRewriteFixtureTest.class
                .getResourceAsStream("/permission-fixtures/golden-rewrites.json")) {
            List<Case> cases = mapper.readValue(in,
                    mapper.getTypeFactory().constructCollectionType(List.class, Case.class));
            return cases.stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("loadCases")
    void rewritesMatchGolden(Case c) {
        PolicyContext ctx = new PolicyContext();
        ctx.setUser(User.get(0L, c.user));
        ctx.setRowPolicies(c.rowPolicies == null ? List.of() : c.rowPolicies);
        ctx.setColumnPolicies(c.columnPolicies == null ? List.of() : c.columnPolicies);

        String sql = c.inputSql;
        sql = new RowLevelPolicyCorrector().rewrite(sql, ctx);
        sql = new ColumnMaskingCorrector().rewrite(sql, ctx);

        assertEquals(normalize(c.expectedSql), normalize(sql), "case=" + c.name);
    }

    private String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
