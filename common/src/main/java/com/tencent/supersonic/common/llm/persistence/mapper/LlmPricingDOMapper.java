package com.tencent.supersonic.common.llm.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmPricingDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LlmPricingDOMapper extends BaseMapper<LlmPricingDO> {
}
