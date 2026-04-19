package com.tencent.supersonic.headless.core.translator.corrector.policy;

import com.tencent.supersonic.common.pojo.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Fixture-only resolver used by tests and for Task 2 wiring. Not a @Component. */
public class InMemoryPolicyResolver implements PolicyResolver {

    private final Map<String, List<RowPolicy>> rowByUser = new HashMap<>();
    private final Map<String, List<ColumnPolicy>> colByUser = new HashMap<>();

    public void register(String userName, RowPolicy policy) {
        rowByUser.computeIfAbsent(userName, k -> new ArrayList<>()).add(policy);
    }

    public void register(String userName, ColumnPolicy policy) {
        colByUser.computeIfAbsent(userName, k -> new ArrayList<>()).add(policy);
    }

    @Override
    public List<RowPolicy> resolveRowPolicies(User user, Set<Long> modelIds, Long dataSetId) {
        return rowByUser.getOrDefault(user.getName(), List.of()).stream().filter(
                p -> modelIds == null || modelIds.isEmpty() || modelIds.contains(p.getModelId()))
                .toList();
    }

    @Override
    public List<ColumnPolicy> resolveColumnPolicies(User user, Set<Long> modelIds, Long dataSetId) {
        return colByUser.getOrDefault(user.getName(), List.of()).stream().filter(
                p -> modelIds == null || modelIds.isEmpty() || modelIds.contains(p.getModelId()))
                .toList();
    }
}
