package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.TenantQuotaDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis mapper for tenant quota table.
 */
@Mapper
public interface TenantQuotaMapper extends BaseMapper<TenantQuotaDO> {
}
