package com.tencent.supersonic.billing.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.billing.server.persistence.dataobject.TenantSubscriptionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis mapper for tenant subscription table.
 */
@Mapper
public interface TenantSubscriptionDOMapper extends BaseMapper<TenantSubscriptionDO> {
}
