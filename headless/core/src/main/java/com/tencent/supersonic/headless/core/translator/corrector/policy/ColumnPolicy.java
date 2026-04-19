package com.tencent.supersonic.headless.core.translator.corrector.policy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnPolicy {
    private String policyId;
    private Long modelId;
    private String columnBizName;
    /** Pattern with a single %s placeholder, e.g. "CONCAT(LEFT(%s,3),'****')". */
    private String maskTemplate;
}
