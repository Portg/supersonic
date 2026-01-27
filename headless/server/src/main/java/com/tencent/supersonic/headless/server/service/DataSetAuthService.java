package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.DataSetAuthGroup;

import java.util.List;

public interface DataSetAuthService {

    List<DataSetAuthGroup> queryAuthGroups(Long datasetId, Long groupId);

    DataSetAuthGroup createAuthGroup(DataSetAuthGroup group, User user);

    void updateAuthGroup(DataSetAuthGroup group, User user);

    void removeAuthGroup(Long groupId, User user);

    AuthorizedResourceResp queryAuthorizedResources(Long datasetId, User user);

    boolean checkDataSetViewPermission(Long datasetId, User user);

    boolean checkDataSetAdminPermission(Long datasetId, User user);

    List<String> getRowFilters(Long datasetId, User user);
}
