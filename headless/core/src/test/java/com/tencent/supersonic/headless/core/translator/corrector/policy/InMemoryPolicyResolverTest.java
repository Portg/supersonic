package com.tencent.supersonic.headless.core.translator.corrector.policy;

import com.tencent.supersonic.common.pojo.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryPolicyResolverTest {

    @Test
    void returnsEmptyWhenNoFixtureRegistered() {
        InMemoryPolicyResolver resolver = new InMemoryPolicyResolver();
        List<RowPolicy> rows = resolver.resolveRowPolicies(User.get(0L, "alice"), Set.of(1L));
        assertTrue(rows.isEmpty());
    }

    @Test
    void returnsRegisteredPolicyForMatchingUserAndModel() {
        InMemoryPolicyResolver resolver = new InMemoryPolicyResolver();
        RowPolicy p =
                new RowPolicy("P1", 1L, List.of("s2_user_orders"), "region = 'APAC'", "APAC only");
        resolver.register("alice", p);

        List<RowPolicy> rows = resolver.resolveRowPolicies(User.get(0L, "alice"), Set.of(1L));
        assertEquals(1, rows.size());
        assertEquals("P1", rows.get(0).getPolicyId());
    }

    @Test
    void filtersByRequestedModelIds() {
        InMemoryPolicyResolver resolver = new InMemoryPolicyResolver();
        resolver.register("alice", new RowPolicy("P1", 1L, List.of("t1"), "x=1", null));
        resolver.register("alice", new RowPolicy("P2", 2L, List.of("t2"), "y=2", null));

        List<RowPolicy> rows = resolver.resolveRowPolicies(User.get(0L, "alice"), Set.of(1L));
        assertEquals(1, rows.size());
        assertEquals("P1", rows.get(0).getPolicyId());
    }

    @Test
    void returnsEmptyColumnPoliciesWhenNoFixtureRegistered() {
        InMemoryPolicyResolver resolver = new InMemoryPolicyResolver();
        List<ColumnPolicy> cols = resolver.resolveColumnPolicies(User.get(0L, "alice"), Set.of(1L));
        assertTrue(cols.isEmpty());
    }

    @Test
    void returnsRegisteredColumnPolicyForMatchingUser() {
        InMemoryPolicyResolver resolver = new InMemoryPolicyResolver();
        ColumnPolicy p = new ColumnPolicy("CP1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')");
        resolver.register("alice", p);

        List<ColumnPolicy> cols = resolver.resolveColumnPolicies(User.get(0L, "alice"), Set.of(1L));
        assertEquals(1, cols.size());
        assertEquals("CP1", cols.get(0).getPolicyId());
    }

    @Test
    void filtersByRequestedModelIdsForColumnPolicies() {
        InMemoryPolicyResolver resolver = new InMemoryPolicyResolver();
        resolver.register("alice",
                new ColumnPolicy("CP1", 1L, "phone", "CONCAT(LEFT(%s,3),'****')"));
        resolver.register("alice",
                new ColumnPolicy("CP2", 2L, "email", "CONCAT(LEFT(%s,3),'****')"));

        List<ColumnPolicy> cols = resolver.resolveColumnPolicies(User.get(0L, "alice"), Set.of(1L));
        assertEquals(1, cols.size());
        assertEquals("CP1", cols.get(0).getPolicyId());
    }
}
