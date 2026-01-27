package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.OAuthStateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OAuthStateDOMapper extends BaseMapper<OAuthStateDO> {

}
