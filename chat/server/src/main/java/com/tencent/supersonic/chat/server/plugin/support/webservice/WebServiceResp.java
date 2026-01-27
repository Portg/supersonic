package com.tencent.supersonic.chat.server.plugin.support.webservice;

import com.tencent.supersonic.chat.server.plugin.support.WebBase;
import lombok.Data;

@Data
public class WebServiceResp {

    private WebBase webBase;

    private Object result;
}
