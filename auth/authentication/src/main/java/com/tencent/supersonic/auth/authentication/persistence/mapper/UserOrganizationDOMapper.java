package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserOrganizationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * UserOrganizationDO Mapper - uses MyBatis-Plus BaseMapper for CRUD operations.
 */
@Mapper
public interface UserOrganizationDOMapper extends BaseMapper<UserOrganizationDO> {
}
