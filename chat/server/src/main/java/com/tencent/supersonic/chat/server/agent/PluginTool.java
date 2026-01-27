package com.tencent.supersonic.chat.server.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class PluginTool extends AgentTool {

    private List<Long> plugins;
}
