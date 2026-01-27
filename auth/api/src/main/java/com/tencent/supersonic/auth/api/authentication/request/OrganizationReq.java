package com.tencent.supersonic.auth.api.authentication.request;

import lombok.Data;

@Data
public class OrganizationReq {

    /**
     * Parent organization id, 0 for root
     */
    private Long parentId;

    /**
     * Organization name
     */
    private String name;

    /**
     * Sort order
     */
    private Integer sortOrder;

    /**
     * Status: 1=enabled, 0=disabled
     */
    private Integer status;
}
