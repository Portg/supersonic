package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.TenantUsageDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis mapper for tenant usage table.
 */
@Mapper
public interface TenantUsageDOMapper extends BaseMapper<TenantUsageDO> {
}
