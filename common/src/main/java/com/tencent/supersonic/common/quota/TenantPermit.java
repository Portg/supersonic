package com.tencent.supersonic.common.quota;

import java.util.concurrent.Semaphore;

public final class TenantPermit implements AutoCloseable {

    private final Semaphore semaphore;
    private final Long tenantId;
    private boolean released;

    public TenantPermit(Semaphore semaphore, Long tenantId) {
        this.semaphore = semaphore;
        this.tenantId = tenantId;
    }

    public static TenantPermit noop() {
        return new TenantPermit(null, null);
    }

    public Long getTenantId() {
        return tenantId;
    }

    @Override
    public void close() {
        if (released || semaphore == null) {
            return;
        }
        released = true;
        semaphore.release();
    }
}
