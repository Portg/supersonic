package com.tencent.supersonic.common.outbox;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OutboxEventService extends ServiceImpl<OutboxMapper, OutboxEvent> {
}
