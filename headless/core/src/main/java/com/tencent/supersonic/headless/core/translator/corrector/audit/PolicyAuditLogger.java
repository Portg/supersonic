package com.tencent.supersonic.headless.core.translator.corrector.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class PolicyAuditLogger {

    /** Dedicated logger name; ops can route this to a separate file/topic. */
    public static final String AUDIT_LOGGER = "s2.permission.audit";

    private static final Logger AUDIT = LoggerFactory.getLogger(AUDIT_LOGGER);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void log(PolicyAuditEntry entry) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ts", Instant.now().toString());
            m.put("policyId", entry.getPolicyId());
            m.put("user", entry.getUser());
            m.put("policyType", entry.getPolicyType());
            m.put("sqlDigest", entry.getSqlDigest());
            AUDIT.info(MAPPER.writeValueAsString(m));
        } catch (JsonProcessingException e) {
            log.warn("audit log serialisation failed", e);
        }
    }

    public static String digest(String sql) {
        if (sql == null)
            return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(md.digest(sql.getBytes())).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
