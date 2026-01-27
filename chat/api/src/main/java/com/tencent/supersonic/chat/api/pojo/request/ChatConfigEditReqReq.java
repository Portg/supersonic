package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatConfigEditReqReq extends ChatConfigBaseReq {

    private Long id;
}
