package com.tencent.supersonic.headless.core.translator.corrector.policy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RowPolicy {
    private String policyId;
    private Long modelId;
    private List<String> tableBizNames;
    private String filterExpression;
    private String description;
}
