package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.TenantDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis mapper for tenant table.
 */
@Mapper
public interface TenantDOMapper extends BaseMapper<TenantDO> {
}
