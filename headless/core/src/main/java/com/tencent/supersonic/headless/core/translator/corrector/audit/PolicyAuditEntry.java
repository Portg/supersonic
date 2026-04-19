package com.tencent.supersonic.headless.core.translator.corrector.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyAuditEntry {
    private String policyId;
    private String user;
    /** "row" | "column" */
    private String policyType;
    private String sqlBefore;
    private String sqlAfter;
    private String sqlDigest;
}
