package com.tencent.supersonic.billing.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.billing.server.persistence.dataobject.SubscriptionPlanDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis mapper for subscription plan table.
 */
@Mapper
public interface SubscriptionPlanDOMapper extends BaseMapper<SubscriptionPlanDO> {
}
