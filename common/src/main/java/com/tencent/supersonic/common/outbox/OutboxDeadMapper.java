package com.tencent.supersonic.common.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboxDeadMapper extends BaseMapper<OutboxDeadEvent> {
}
