package com.tencent.supersonic.auth.authentication.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_role_permission")
public class RolePermissionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;

    private Long permissionId;

    private Date createdAt;
}
