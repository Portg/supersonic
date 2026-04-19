package com.tencent.supersonic.headless.core.translator.corrector;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.core.translator.corrector.policy.ColumnPolicy;
import com.tencent.supersonic.headless.core.translator.corrector.policy.RowPolicy;
import lombok.Data;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
public class PolicyContext {
    private User user;
    private Set<Long> modelIds;
    private Long dataSetId;
    @NonNull
    private List<RowPolicy> rowPolicies = new ArrayList<>();
    @NonNull
    private List<ColumnPolicy> columnPolicies = new ArrayList<>();
    /** If true, do not rewrite; only compute what would change (shadow mode). */
    private boolean shadowMode;
    private boolean auditLogEnabled = true;
}
