package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.OrganizationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * OrganizationDO Mapper - uses MyBatis-Plus BaseMapper for CRUD operations.
 */
@Mapper
public interface OrganizationDOMapper extends BaseMapper<OrganizationDO> {
    // All CRUD operations are provided by BaseMapper
}
