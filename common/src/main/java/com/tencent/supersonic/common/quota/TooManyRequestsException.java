package com.tencent.supersonic.common.quota;

public class TooManyRequestsException extends RuntimeException {

    private final Long tenantId;
    private final int retryAfterSeconds;

    public TooManyRequestsException(Long tenantId, int retryAfterSeconds) {
        super("Tenant " + tenantId + " exceeded concurrency quota; retry after " + retryAfterSeconds
                + "s");
        this.tenantId = tenantId;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
