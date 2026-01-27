package com.tencent.supersonic.auth.api.authorization.request;

import com.tencent.supersonic.auth.api.authorization.pojo.AuthGroup;
import lombok.Data;

import java.util.List;

@Data
public class BatchAuthGroupReq {

    private List<AuthGroup> authGroups;
}
