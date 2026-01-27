package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.server.executor.SemanticDeployExecutor;
import com.tencent.supersonic.headless.server.persistence.dataobject.SemanticDeploymentDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.SemanticTemplateDO;
import com.tencent.supersonic.headless.server.persistence.mapper.SemanticDeploymentMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.SemanticTemplateMapper;
import com.tencent.supersonic.headless.server.pojo.SemanticDeployParam;
import com.tencent.supersonic.headless.server.pojo.SemanticDeployResult;
import com.tencent.supersonic.headless.server.pojo.SemanticDeployment;
import com.tencent.supersonic.headless.server.pojo.SemanticPreviewResult;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplate;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateListResp;
import com.tencent.supersonic.headless.server.service.SemanticTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SemanticTemplateServiceImpl extends
        ServiceImpl<SemanticTemplateMapper, SemanticTemplateDO> implements SemanticTemplateService {

    private static final Long DEFAULT_TENANT_ID = 1L;

    /**
     * Template status constants: - DRAFT (0): Newly created, can be edited/deleted - DEPLOYED (1):
     * Has been deployed, cannot be edited/deleted
     */
    private static final Integer STATUS_DRAFT = 0;
    private static final Integer STATUS_DEPLOYED = 1;

    @Autowired
    private SemanticTemplateMapper templateMapper;

    @Autowired
    private SemanticDeploymentMapper deploymentMapper;

    @Lazy
    @Autowired
    private SemanticDeployExecutor deployExecutor;

    @Override
    public SemanticTemplateListResp getTemplateList(User user) {
        Long tenantId = getTenantId(user);

        // Get builtin templates (is_builtin=1)
        // Do NOT filter by status - status is only for permission checks, not for listing
        QueryWrapper<SemanticTemplateDO> builtinWrapper = new QueryWrapper<>();
        builtinWrapper.lambda().eq(SemanticTemplateDO::getIsBuiltin, 1)
                .orderByDesc(SemanticTemplateDO::getCreatedAt);
        List<SemanticTemplateDO> builtinDOs = templateMapper.selectList(builtinWrapper);
        List<SemanticTemplate> builtinTemplates =
                builtinDOs.stream().map(this::convertToTemplate).collect(Collectors.toList());

        // Get tenant's custom templates (is_builtin=0 AND tenant_id matches)
        // Do NOT filter by status - show all templates (both draft and deployed)
        QueryWrapper<SemanticTemplateDO> customWrapper = new QueryWrapper<>();
        customWrapper.lambda().eq(SemanticTemplateDO::getIsBuiltin, 0)
                .eq(SemanticTemplateDO::getTenantId, tenantId)
                .orderByDesc(SemanticTemplateDO::getCreatedAt);
        List<SemanticTemplateDO> customDOs = templateMapper.selectList(customWrapper);
        List<SemanticTemplate> customTemplates =
                customDOs.stream().map(this::convertToTemplate).collect(Collectors.toList());
        return new SemanticTemplateListResp(builtinTemplates, customTemplates);
    }

    @Override
    public SemanticTemplate getTemplateById(Long id, User user) {
        SemanticTemplateDO templateDO = templateMapper.selectById(id);
        if (templateDO == null) {
            throw new InvalidArgumentException("Template not found: " + id);
        }
        // Check access permission:
        // - Builtin templates (is_builtin=1) are accessible to everyone
        // - Custom templates are only accessible to their own tenant
        Long tenantId = getTenantId(user);
        boolean isBuiltin = templateDO.getIsBuiltin() != null && templateDO.getIsBuiltin() == 1;
        if (!isBuiltin && !templateDO.getTenantId().equals(tenantId)) {
            throw new InvalidArgumentException("No permission to access this template");
        }
        return convertToTemplate(templateDO);
    }

    @Override
    @Transactional
    public SemanticTemplate createTemplate(SemanticTemplate template, User user) {
        Long tenantId = getTenantId(user);
        template.setTenantId(tenantId);
        template.setIsBuiltin(false);
        template.setCreatedBy(user.getName());
        template.setCreatedAt(new Date());
        // New templates start as draft (status=0), become deployed (status=1) after deployment
        template.setStatus(STATUS_DRAFT);

        SemanticTemplateDO templateDO = convertToDO(template);
        templateMapper.insert(templateDO);
        template.setId(templateDO.getId());
        return template;
    }

    @Override
    @Transactional
    public SemanticTemplate updateTemplate(SemanticTemplate template, User user) {
        SemanticTemplateDO existingDO = templateMapper.selectById(template.getId());
        if (existingDO == null) {
            throw new InvalidArgumentException("Template not found: " + template.getId());
        }

        Long tenantId = getTenantId(user);
        // Check permission: only allow updating own templates or builtin templates for SaaS admin
        if (existingDO.getIsBuiltin() == 1) {
            if (!user.isSuperAdmin()) {
                throw new InvalidArgumentException("Only SaaS admin can update builtin templates");
            }
        } else {
            // Custom templates: check tenant permission
            if (!existingDO.getTenantId().equals(tenantId)) {
                throw new InvalidArgumentException("No permission to update this template");
            }
            // Custom templates can only be edited when in draft status (not yet deployed)
            if (!STATUS_DRAFT.equals(existingDO.getStatus())) {
                throw new InvalidArgumentException("Cannot edit template that has been deployed");
            }
        }

        template.setUpdatedBy(user.getName());
        template.setUpdatedAt(new Date());
        SemanticTemplateDO templateDO = convertToDO(template);
        templateDO.setId(existingDO.getId());
        templateDO.setTenantId(existingDO.getTenantId());
        templateDO.setIsBuiltin(existingDO.getIsBuiltin());
        templateDO.setStatus(existingDO.getStatus()); // Preserve status
        templateMapper.updateById(templateDO);
        return convertToTemplate(templateDO);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id, User user) {
        SemanticTemplateDO templateDO = templateMapper.selectById(id);
        if (templateDO == null) {
            return;
        }

        Long tenantId = getTenantId(user);
        // Only allow deleting own templates (not builtin)
        if (templateDO.getIsBuiltin() == 1) {
            throw new InvalidArgumentException("Cannot delete builtin templates");
        }
        if (!templateDO.getTenantId().equals(tenantId)) {
            throw new InvalidArgumentException("No permission to delete this template");
        }
        // Can only delete templates that haven't been deployed yet
        if (!STATUS_DRAFT.equals(templateDO.getStatus())) {
            throw new InvalidArgumentException("Cannot delete template that has been deployed");
        }

        templateMapper.deleteById(id);
    }

    // ============ Deployment Functions ============

    @Override
    public SemanticPreviewResult previewDeployment(Long templateId, SemanticDeployParam param,
            User user) {
        SemanticTemplate template = getTemplateById(templateId, user);
        return deployExecutor.preview(template, param, user);
    }

    @Override
    @Transactional
    public SemanticDeployment executeDeployment(Long templateId, SemanticDeployParam param,
            User user) {
        SemanticTemplate template = getTemplateById(templateId, user);
        Long tenantId = getTenantId(user);

        // Check if template has already been successfully deployed by this tenant
        if (hasSuccessfulDeployment(templateId, tenantId)) {
            throw new InvalidArgumentException("该模板已成功部署过，不能重复部署。如需重新部署，请先删除之前部署创建的语义对象。");
        }

        // Create deployment record
        SemanticDeployment deployment = new SemanticDeployment();
        deployment.setTemplateId(templateId);
        deployment.setTemplateName(template.getName());
        deployment.setDatabaseId(param.getDatabaseId());
        deployment.setParamConfig(param);
        deployment.setStatus(SemanticDeployment.DeploymentStatus.PENDING);
        deployment.setTenantId(tenantId);
        deployment.setCreatedBy(user.getName());
        deployment.setCreatedAt(new Date());

        SemanticDeploymentDO deploymentDO = convertToDeploymentDO(deployment);
        deploymentMapper.insert(deploymentDO);
        deployment.setId(deploymentDO.getId());

        // Execute deployment
        try {
            deployment.setStatus(SemanticDeployment.DeploymentStatus.RUNNING);
            deployment.setStartTime(new Date());
            updateDeploymentStatus(deployment);

            SemanticDeployResult result = deployExecutor.execute(template, param, user);
            deployment.setResultDetail(result);
            deployment.setStatus(SemanticDeployment.DeploymentStatus.SUCCESS);
            deployment.setEndTime(new Date());

            // Update template status to DEPLOYED after successful deployment
            // Only for custom templates (builtin templates are always status=1)
            SemanticTemplateDO templateDO = templateMapper.selectById(templateId);
            if (templateDO != null && templateDO.getIsBuiltin() == 0) {
                templateDO.setStatus(STATUS_DEPLOYED);
                templateDO.setUpdatedAt(new Date());
                templateDO.setUpdatedBy(user.getName());
                templateMapper.updateById(templateDO);
                log.info("Template {} status updated to DEPLOYED after successful deployment",
                        templateId);
            }
        } catch (Exception e) {
            log.error("Failed to deploy template: {}", templateId, e);
            deployment.setStatus(SemanticDeployment.DeploymentStatus.FAILED);
            deployment.setErrorMessage(e.getMessage());
            deployment.setEndTime(new Date());
        }

        updateDeploymentStatus(deployment);
        return deployment;
    }

    private void updateDeploymentStatus(SemanticDeployment deployment) {
        SemanticDeploymentDO deploymentDO = convertToDeploymentDO(deployment);
        deploymentMapper.updateById(deploymentDO);
    }

    // ============ Deployment History ============

    @Override
    public List<SemanticDeployment> getDeploymentHistory(User user) {
        Long tenantId = getTenantId(user);
        QueryWrapper<SemanticDeploymentDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(SemanticDeploymentDO::getTenantId, tenantId)
                .orderByDesc(SemanticDeploymentDO::getCreatedAt);
        List<SemanticDeploymentDO> deploymentDOs = deploymentMapper.selectList(wrapper);
        return deploymentDOs.stream().map(this::convertToDeployment).collect(Collectors.toList());
    }

    @Override
    public SemanticDeployment getDeploymentById(Long id, User user) {
        SemanticDeploymentDO deploymentDO = deploymentMapper.selectById(id);
        if (deploymentDO == null) {
            throw new InvalidArgumentException("Deployment not found: " + id);
        }

        Long tenantId = getTenantId(user);
        if (!deploymentDO.getTenantId().equals(tenantId) && !user.isSuperAdmin()) {
            throw new InvalidArgumentException("No permission to access this deployment");
        }

        return convertToDeployment(deploymentDO);
    }

    // ============ System Management ============

    @Override
    public void initBuiltinTemplates() {
        // Check if builtin templates already exist
        QueryWrapper<SemanticTemplateDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(SemanticTemplateDO::getIsBuiltin, 1);
        long count = templateMapper.selectCount(wrapper);
        if (count > 0) {
            log.info("Builtin templates already initialized, skipping...");
            return;
        }

        log.info("Initializing builtin templates...");
        // Note: Actual template initialization will be done by BuiltinSemanticTemplateInitializer
    }

    @Override
    public List<SemanticTemplate> getBuiltinTemplates() {
        QueryWrapper<SemanticTemplateDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(SemanticTemplateDO::getIsBuiltin, 1);
        List<SemanticTemplateDO> templateDOs = templateMapper.selectList(wrapper);
        return templateDOs.stream().map(this::convertToTemplate).collect(Collectors.toList());
    }

    @Override
    public List<SemanticDeployment> getAllDeploymentHistory(User user) {
        if (!user.isSuperAdmin()) {
            throw new InvalidArgumentException("Only SaaS admin can view all deployment history");
        }

        QueryWrapper<SemanticDeploymentDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().orderByDesc(SemanticDeploymentDO::getCreatedAt);
        List<SemanticDeploymentDO> deploymentDOs = deploymentMapper.selectList(wrapper);
        return deploymentDOs.stream().map(this::convertToDeployment).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SemanticTemplate saveBuiltinTemplate(SemanticTemplate template, User user) {
        if (!user.isSuperAdmin()) {
            throw new InvalidArgumentException("Only SaaS admin can manage builtin templates");
        }

        template.setTenantId(DEFAULT_TENANT_ID);
        template.setIsBuiltin(true);
        template.setStatus(STATUS_DEPLOYED); // Builtin templates are always in deployed state

        // Check if template with same bizName exists
        QueryWrapper<SemanticTemplateDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(SemanticTemplateDO::getTenantId, DEFAULT_TENANT_ID)
                .eq(SemanticTemplateDO::getBizName, template.getBizName());
        SemanticTemplateDO existingDO = templateMapper.selectOne(wrapper);

        if (existingDO != null) {
            // Update existing
            template.setId(existingDO.getId());
            template.setUpdatedBy(user.getName());
            template.setUpdatedAt(new Date());
            SemanticTemplateDO templateDO = convertToDO(template);
            templateMapper.updateById(templateDO);
        } else {
            // Create new
            template.setCreatedBy(user.getName());
            template.setCreatedAt(new Date());
            // Status already set to STATUS_DEPLOYED above
            template.setStatus(STATUS_DEPLOYED);
            SemanticTemplateDO templateDO = convertToDO(template);
            templateMapper.insert(templateDO);
            template.setId(templateDO.getId());
        }

        return template;
    }

    // ============ Helper Methods ============

    private Long getTenantId(User user) {
        // Priority: TenantContext > User.tenantId > default
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        if (user.getTenantId() != null) {
            return user.getTenantId();
        }
        return DEFAULT_TENANT_ID;
    }

    /**
     * Check if a template has already been successfully deployed by the given tenant.
     */
    private boolean hasSuccessfulDeployment(Long templateId, Long tenantId) {
        QueryWrapper<SemanticDeploymentDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(SemanticDeploymentDO::getTemplateId, templateId)
                .eq(SemanticDeploymentDO::getTenantId, tenantId).eq(SemanticDeploymentDO::getStatus,
                        SemanticDeployment.DeploymentStatus.SUCCESS.name());
        return deploymentMapper.selectCount(wrapper) > 0;
    }

    private SemanticTemplate convertToTemplate(SemanticTemplateDO templateDO) {
        if (templateDO == null) {
            return null;
        }
        SemanticTemplate template = new SemanticTemplate();
        template.setId(templateDO.getId());
        template.setName(templateDO.getName());
        template.setBizName(templateDO.getBizName());
        template.setDescription(templateDO.getDescription());
        template.setCategory(templateDO.getCategory());
        template.setPreviewImage(templateDO.getPreviewImage());
        template.setStatus(templateDO.getStatus());
        template.setIsBuiltin(templateDO.getIsBuiltin() != null && templateDO.getIsBuiltin() == 1);
        template.setTenantId(templateDO.getTenantId());
        template.setCreatedAt(templateDO.getCreatedAt());
        template.setCreatedBy(templateDO.getCreatedBy());
        template.setUpdatedAt(templateDO.getUpdatedAt());
        template.setUpdatedBy(templateDO.getUpdatedBy());

        // Parse template config
        if (StringUtils.isNotBlank(templateDO.getTemplateConfig())) {
            template.setTemplateConfig(JsonUtil.toObject(templateDO.getTemplateConfig(),
                    SemanticTemplateConfig.class));
        }
        return template;
    }

    private SemanticTemplateDO convertToDO(SemanticTemplate template) {
        SemanticTemplateDO templateDO = new SemanticTemplateDO();
        templateDO.setId(template.getId());
        templateDO.setName(template.getName());
        templateDO.setBizName(template.getBizName());
        templateDO.setDescription(template.getDescription());
        templateDO.setCategory(template.getCategory());
        templateDO.setPreviewImage(template.getPreviewImage());
        templateDO.setStatus(template.getStatus());
        templateDO.setIsBuiltin(template.getIsBuiltin() != null && template.getIsBuiltin() ? 1 : 0);
        templateDO.setTenantId(template.getTenantId());
        templateDO.setCreatedAt(template.getCreatedAt());
        templateDO.setCreatedBy(template.getCreatedBy());
        templateDO.setUpdatedAt(template.getUpdatedAt());
        templateDO.setUpdatedBy(template.getUpdatedBy());

        // Serialize template config
        if (template.getTemplateConfig() != null) {
            templateDO.setTemplateConfig(JsonUtil.toString(template.getTemplateConfig()));
        }
        return templateDO;
    }

    private SemanticDeployment convertToDeployment(SemanticDeploymentDO deploymentDO) {
        if (deploymentDO == null) {
            return null;
        }
        SemanticDeployment deployment = new SemanticDeployment();
        deployment.setId(deploymentDO.getId());
        deployment.setTemplateId(deploymentDO.getTemplateId());
        deployment.setTemplateName(deploymentDO.getTemplateName());
        deployment.setDatabaseId(deploymentDO.getDatabaseId());
        deployment.setStatus(SemanticDeployment.DeploymentStatus.valueOf(deploymentDO.getStatus()));
        deployment.setErrorMessage(deploymentDO.getErrorMessage());
        deployment.setStartTime(deploymentDO.getStartTime());
        deployment.setEndTime(deploymentDO.getEndTime());
        deployment.setTenantId(deploymentDO.getTenantId());
        deployment.setCreatedAt(deploymentDO.getCreatedAt());
        deployment.setCreatedBy(deploymentDO.getCreatedBy());

        // Parse param config
        if (StringUtils.isNotBlank(deploymentDO.getParamConfig())) {
            deployment.setParamConfig(
                    JsonUtil.toObject(deploymentDO.getParamConfig(), SemanticDeployParam.class));
        }
        // Parse result detail
        if (StringUtils.isNotBlank(deploymentDO.getResultDetail())) {
            deployment.setResultDetail(
                    JsonUtil.toObject(deploymentDO.getResultDetail(), SemanticDeployResult.class));
        }
        return deployment;
    }

    private SemanticDeploymentDO convertToDeploymentDO(SemanticDeployment deployment) {
        SemanticDeploymentDO deploymentDO = new SemanticDeploymentDO();
        deploymentDO.setId(deployment.getId());
        deploymentDO.setTemplateId(deployment.getTemplateId());
        deploymentDO.setTemplateName(deployment.getTemplateName());
        deploymentDO.setDatabaseId(deployment.getDatabaseId());
        deploymentDO.setStatus(deployment.getStatus().name());
        deploymentDO.setErrorMessage(deployment.getErrorMessage());
        deploymentDO.setStartTime(deployment.getStartTime());
        deploymentDO.setEndTime(deployment.getEndTime());
        deploymentDO.setTenantId(deployment.getTenantId());
        deploymentDO.setCreatedAt(deployment.getCreatedAt());
        deploymentDO.setCreatedBy(deployment.getCreatedBy());

        // Serialize param config
        if (deployment.getParamConfig() != null) {
            deploymentDO.setParamConfig(JsonUtil.toString(deployment.getParamConfig()));
        }
        // Serialize result detail
        if (deployment.getResultDetail() != null) {
            deploymentDO.setResultDetail(JsonUtil.toString(deployment.getResultDetail()));
        }
        return deploymentDO;
    }
}
