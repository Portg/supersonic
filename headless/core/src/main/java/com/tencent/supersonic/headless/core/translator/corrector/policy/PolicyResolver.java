package com.tencent.supersonic.headless.core.translator.corrector.policy;

import com.tencent.supersonic.common.pojo.User;

import java.util.List;
import java.util.Set;

public interface PolicyResolver {

    List<RowPolicy> resolveRowPolicies(User user, Set<Long> modelIds, Long dataSetId);

    List<ColumnPolicy> resolveColumnPolicies(User user, Set<Long> modelIds, Long dataSetId);
}
