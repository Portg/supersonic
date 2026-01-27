package com.tencent.supersonic.headless.server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for semantic template list, separating builtin and custom templates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SemanticTemplateListResp {

    /**
     * System builtin templates, visible to all tenants
     */
    private List<SemanticTemplate> builtinTemplates;

    /**
     * Tenant's custom templates
     */
    private List<SemanticTemplate> customTemplates;
}
