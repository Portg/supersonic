package com.tencent.supersonic.auth.api.authentication.request;

import lombok.Data;

import java.util.List;

@Data
public class UserOrganizationReq {

    /**
     * User id (for single user operation)
     */
    private Long userId;

    /**
     * User ids (for batch operation)
     */
    private List<Long> userIds;

    /**
     * Organization id
     */
    private Long organizationId;

    /**
     * Whether this is the primary organization for the user
     */
    private Boolean isPrimary;
}
