package com.tencent.supersonic.chat.server.listener;

import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.plugin.ChatPlugin;
import com.tencent.supersonic.chat.server.plugin.PluginParseConfig;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.PluginService;
import com.tencent.supersonic.common.config.ChatModel;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.service.ChatModelService;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.server.event.TemplateDeployedEvent;
import com.tencent.supersonic.headless.server.pojo.SemanticDeployResult;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.PluginConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TemplateDeployedEventListener {

    private final AgentService agentService;
    private final PluginService pluginService;
    private final ChatModelService chatModelService;

    public TemplateDeployedEventListener(AgentService agentService, PluginService pluginService,
            ChatModelService chatModelService) {
        this.agentService = agentService;
        this.pluginService = pluginService;
        this.chatModelService = chatModelService;
    }

    @EventListener
    public void onTemplateDeployed(TemplateDeployedEvent event) {
        SemanticTemplateConfig config = event.getConfig();
        SemanticDeployResult result = event.getResult();
        User user = event.getUser();

        // Use result.getAgentConfig() which has resolved parameter placeholders
        if (result.getAgentConfig() != null) {
            createAgent(result.getAgentConfig(), result, user);
        }

        if (!CollectionUtils.isEmpty(config.getPlugins())) {
            createPlugins(config.getPlugins(), user);
        }
    }

    private void createAgent(SemanticDeployResult.AgentConfigResult agentConfig,
            SemanticDeployResult result, User user) {
        try {
            Agent agent = new Agent();
            agent.setName(agentConfig.getName());
            agent.setDescription(agentConfig.getDescription());
            agent.setStatus(1);
            agent.setEnableSearch(Boolean.TRUE.equals(agentConfig.getEnableSearch()) ? 1 : 0);
            agent.setExamples(agentConfig.getExamples() != null ? agentConfig.getExamples()
                    : Collections.emptyList());
            agent.setAdmins(Collections.singletonList(user.getName()));

            // Build toolConfig JSON
            if (agentConfig.getDataSetId() != null) {
                agent.setToolConfig(String.format(
                        "{\"tools\":[{\"id\":\"1\",\"type\":\"DATASET\",\"dataSetIds\":[%d]}]}",
                        agentConfig.getDataSetId()));
            } else {
                agent.setToolConfig("{}");
            }

            // Configure chatAppOverrides if present
            if (agentConfig.getChatAppOverrides() != null
                    && !agentConfig.getChatAppOverrides().isEmpty()) {
                Map<String, ChatApp> allApps =
                        new HashMap<>(ChatAppManager.getAllApps(AppModule.CHAT));
                setChatModelForApps(allApps, user);
                applyOverrides(allApps, agentConfig.getChatAppOverrides());
                agent.setChatAppConfig(allApps);
            }

            Agent created = agentService.createAgent(agent, user);
            agentConfig.setAgentId(created.getId());
            log.info("Auto-created Agent '{}' with ID: {}", agentConfig.getName(), created.getId());
        } catch (Exception e) {
            log.warn(
                    "Failed to auto-create Agent '{}': {}. "
                            + "Please create it manually through the chat module.",
                    agentConfig.getName(), e.getMessage());
        }
    }

    private void setChatModelForApps(Map<String, ChatApp> allApps, User user) {
        Integer chatModelId = 0;
        List<ChatModel> chatModels = chatModelService.getChatModels(User.getDefaultUser());
        if (!chatModels.isEmpty()) {
            chatModelId = chatModels.getFirst().getId().intValue();
        }
        for (ChatApp app : allApps.values()) {
            app.setChatModelId(chatModelId);
        }
    }

    private void applyOverrides(Map<String, ChatApp> allApps,
            Map<String, Boolean> chatAppOverrides) {
        for (Map.Entry<String, ChatApp> entry : allApps.entrySet()) {
            Boolean override = chatAppOverrides.get(entry.getKey());
            if (override != null) {
                entry.getValue().setEnable(override);
            }
        }
    }

    private void createPlugins(List<PluginConfig> pluginConfigs, User user) {
        for (PluginConfig pc : pluginConfigs) {
            try {
                ChatPlugin plugin = new ChatPlugin();
                plugin.setType(pc.getType());
                plugin.setName(pc.getName());
                plugin.setPattern(pc.getPattern());
                plugin.setDataSetList(pc.getDataSetIds());
                plugin.setConfig(JsonUtil.toString(pc.getConfig()));

                PluginParseConfig parseConfig = PluginParseConfig.builder().name(pc.getName())
                        .description(pc.getDescription()).examples(pc.getExamples()).build();
                plugin.setParseModeConfig(JsonUtil.toString(parseConfig));

                pluginService.createPlugin(plugin, user);
                log.info("Auto-created Plugin '{}'", pc.getName());
            } catch (Exception e) {
                log.warn("Failed to auto-create Plugin '{}': {}", pc.getName(), e.getMessage());
            }
        }
    }
}
