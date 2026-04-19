package com.tencent.supersonic.headless.core.translator.corrector;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "s2.permission.corrector")
@Data
public class PolicyCorrectorProperties {
    /** When true, corrector chain runs but does NOT rewrite; old aspect still active. */
    private boolean shadowMode = true;
    /** Master kill-switch: false disables the whole chain. */
    private boolean enabled = true;
    private boolean auditLogEnabled = true;
}
