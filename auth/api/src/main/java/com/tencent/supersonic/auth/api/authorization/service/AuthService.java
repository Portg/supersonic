package com.tencent.supersonic.auth.api.authorization.service;

import com.tencent.supersonic.auth.api.authorization.pojo.AuthGroup;
import com.tencent.supersonic.auth.api.authorization.request.BatchAuthorizeReq;
import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.auth.api.authorization.response.BatchOperationResult;
import com.tencent.supersonic.common.pojo.User;

import java.util.List;

public interface AuthService {

    List<AuthGroup> queryAuthGroups(String domainId, Integer groupId);

    void addOrUpdateAuthGroup(AuthGroup group);

    void removeAuthGroup(AuthGroup group);

    AuthorizedResourceResp queryAuthorizedResources(QueryAuthResReq req, User user);

    BatchOperationResult batchCreateAuthGroups(List<AuthGroup> groups);

    BatchOperationResult batchUpdateAuthGroups(List<AuthGroup> groups);

    BatchOperationResult batchRemoveAuthGroups(List<Integer> groupIds);

    BatchOperationResult batchAuthorize(BatchAuthorizeReq req);

    BatchOperationResult batchRevokeAuthorize(BatchAuthorizeReq req);
}
