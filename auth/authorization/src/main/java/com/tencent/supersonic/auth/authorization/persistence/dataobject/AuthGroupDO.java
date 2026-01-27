package com.tencent.supersonic.auth.authorization.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("s2_auth_groups")
public class AuthGroupDO {

    @TableId(type = IdType.INPUT)
    private Integer groupId;

    private String config;
}
