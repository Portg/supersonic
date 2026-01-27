package com.tencent.supersonic.auth.authorization.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.authorization.persistence.dataobject.AuthGroupDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthGroupDOMapper extends BaseMapper<AuthGroupDO> {
}
