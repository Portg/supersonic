package com.tencent.supersonic.common.quota;

import java.util.Map;

public interface TenantQuotaService {

    TenantPermit acquireJdbc(Long tenantId, long timeoutMs);

    void refresh(Long tenantId);

    Map<Long, Integer> availablePermits();

    Map<Long, Integer> waitingThreads();
}
