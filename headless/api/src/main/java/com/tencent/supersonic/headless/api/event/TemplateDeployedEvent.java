package com.tencent.supersonic.headless.api.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.SemanticDeployResult;
import com.tencent.supersonic.headless.api.pojo.SemanticTemplateConfig;
import org.springframework.context.ApplicationEvent;

public class TemplateDeployedEvent extends ApplicationEvent {

    private SemanticDeployResult result;
    private SemanticTemplateConfig config;
    private User user;

    /** Jackson deserialization constructor. */
    @JsonCreator
    public TemplateDeployedEvent() {
        super("outbox");
    }

    public TemplateDeployedEvent(Object source, SemanticDeployResult result,
            SemanticTemplateConfig config, User user) {
        super(source);
        this.result = result;
        this.config = config;
        this.user = user;
    }

    public SemanticDeployResult getResult() {
        return result;
    }

    public void setResult(SemanticDeployResult result) {
        this.result = result;
    }

    public SemanticTemplateConfig getConfig() {
        return config;
    }

    public void setConfig(SemanticTemplateConfig config) {
        this.config = config;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
