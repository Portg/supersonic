package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.OAuthProviderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OAuthProviderDOMapper extends BaseMapper<OAuthProviderDO> {

}
