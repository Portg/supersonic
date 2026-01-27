package com.tencent.supersonic.auth.authentication.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.auth.authentication.persistence.dataobject.UserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * UserDO Mapper - uses MyBatis-Plus BaseMapper for CRUD operations.
 */
@Mapper
public interface UserDOMapper extends BaseMapper<UserDO> {

}
