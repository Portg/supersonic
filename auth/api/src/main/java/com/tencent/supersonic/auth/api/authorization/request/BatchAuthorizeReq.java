package com.tencent.supersonic.auth.api.authorization.request;

import lombok.Data;

import java.util.List;

@Data
public class BatchAuthorizeReq {

    private List<Integer> groupIds;
    private List<String> users;
    private List<String> departmentIds;
    /** ADD or REMOVE */
    private String operationType;
}
