package com.tencent.supersonic.common.metrics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TenantTagNormalizer {

    public static final String OTHER = "other";
    public static final String NONE = "none";
    public static final String DISABLED = "disabled";

    private final Set<String> allowlist;
    private final int limit;
    private final boolean emit;
    private final Set<String> dynamicAdmitted = ConcurrentHashMap.newKeySet();

    public TenantTagNormalizer(
            @Value("${s2.observability.nl2sql.top-tenants:#{T(java.util.Collections).emptyList()}}") List<String> topTenants,
            @Value("${s2.observability.nl2sql.tenant-tag-limit:50}") int limit,
            @Value("${s2.observability.nl2sql.emit-tenant-tag:true}") boolean emit) {
        this.allowlist = Set.copyOf(topTenants);
        this.limit = Math.max(1, limit);
        this.emit = emit;
    }

    public String normalize(String tenantId) {
        if (!emit) {
            return DISABLED;
        }
        if (tenantId == null || tenantId.isBlank()) {
            return NONE;
        }
        if (allowlist.contains(tenantId)) {
            return tenantId;
        }
        if (dynamicAdmitted.contains(tenantId)) {
            return tenantId;
        }
        if (allowlist.size() + dynamicAdmitted.size() < limit) {
            dynamicAdmitted.add(tenantId);
            return tenantId;
        }
        return OTHER;
    }
}
