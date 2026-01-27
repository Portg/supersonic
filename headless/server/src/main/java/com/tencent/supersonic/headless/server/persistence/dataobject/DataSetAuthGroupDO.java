package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_dataset_auth_groups")
public class DataSetAuthGroupDO {

    @TableId(type = IdType.AUTO)
    private Long groupId;

    private Long datasetId;

    private String name;

    private String authRules;

    private String dimensionFilters;

    private String dimensionFilterDescription;

    private String authorizedUsers;

    private String authorizedDepartmentIds;

    private Integer inheritFromModel;

    private Long tenantId;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;
}
