package com.tencent.supersonic.headless.api.pojo;

import com.tencent.supersonic.auth.api.authorization.pojo.AuthRule;
import lombok.Data;

import java.util.List;

@Data
public class DataSetAuthGroup {

    private Long groupId;

    private Long datasetId;

    private String name;

    private List<AuthRule> authRules;

    private List<String> dimensionFilters;

    private String dimensionFilterDescription;

    private List<String> authorizedUsers;

    private List<String> authorizedDepartmentIds;

    private Integer inheritFromModel = 1;
}
