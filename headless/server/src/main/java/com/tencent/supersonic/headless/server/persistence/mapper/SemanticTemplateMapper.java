package com.tencent.supersonic.headless.server.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.headless.server.persistence.dataobject.SemanticTemplateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SemanticTemplateMapper extends BaseMapper<SemanticTemplateDO> {
}
