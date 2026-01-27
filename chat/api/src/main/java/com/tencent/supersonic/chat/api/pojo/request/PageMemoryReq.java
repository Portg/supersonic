package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.common.pojo.PageBaseReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PageMemoryReq extends PageBaseReq {

    private ChatMemoryFilter chatMemoryFilter = new ChatMemoryFilter();
}
