package com.tencent.supersonic.headless.core.translator.corrector.shadow;

import com.tencent.supersonic.headless.core.translator.corrector.audit.PolicyAuditLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class ShadowModeComparator {

    public void compare(String oldSql, String newSql, String user) {
        if (Objects.equals(normalize(oldSql), normalize(newSql)))
            return;
        log.warn("shadow-diff user={} oldDigest={} newDigest={} oldSql={} newSql={}", user,
                PolicyAuditLogger.digest(oldSql), PolicyAuditLogger.digest(newSql), oldSql, newSql);
    }

    private String normalize(String sql) {
        return sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
    }
}
