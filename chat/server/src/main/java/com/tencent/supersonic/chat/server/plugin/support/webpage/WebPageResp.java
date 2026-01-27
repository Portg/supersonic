package com.tencent.supersonic.chat.server.plugin.support.webpage;

import com.tencent.supersonic.chat.server.plugin.support.WebBase;
import lombok.Data;

import java.util.List;

@Data
public class WebPageResp {

    private Long pluginId;

    private String pluginType;

    private String name;

    private String description;

    private WebBase webPage;

    private List<WebBase> moreWebPage;
}
