package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.common.config.TenantConfig;
import com.tencent.supersonic.common.context.TenantContext;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.server.event.TemplateDeployedEvent;
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
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SemanticTemplateServiceImpl extends
        ServiceImpl<SemanticTemplateMapper, SemanticTemplateDO> implements SemanticTemplateService {

    private static final Integer STATUS_DRAFT = 0;
    private static final Integer STATUS_DEPLOYED = 1;

    private final SemanticDeploymentMapper deploymentMapper;
    private final SemanticDeployExecutor deployExecutor;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TenantConfig tenantConfig;

    public SemanticTemplateServiceImpl(SemanticDeploymentMapper deploymentMapper,
            @Lazy SemanticDeployExecutor deployExecutor,
            ApplicationEventPublisher applicationEventPublisher, TenantConfig tenantConfig) {
        this.deploymentMapper = deploymentMapper;
        this.deployExecutor = deployExecutor;
        this.applicationEventPublisher = applicationEventPublisher;
        this.tenantConfig = tenantConfig;
    }

    @Override
    public SemanticTemplateListResp getTemplateList(User user) {
        Long tenantId = getTenantId(user);

        QueryWrapper<SemanticTemplateDO> builtinWrapper = new QueryWrapper<>();
        builtinWrapper.lambda().eq(SemanticTemplateDO::getIsBuiltin, 1)
                .orderByDesc(SemanticTemplateDO::getCreatedAt);
        List<SemanticTemplateDO> builtinDOs = baseMapper.selectList(builtinWrapper);
        List<SemanticTemplate> builtinTemplates =
                builtinDOs.stream().map(this::convertToTemplate).collect(Collectors.toList());

        QueryWrapper<SemanticTemplateDO> customWrapper = new QueryWrapper<>();
        customWrapper.lambda().eq(SemanticTemplateDO::getIsBuiltin, 0)
                .eq(SemanticTemplateDO::getTenantId, tenantId)
                .orderByDesc(SemanticTemplateDO::getCreatedAt);
        List<SemanticTemplateDO> customDOs = baseMapper.selectList(customWrapper);
        List<SemanticTemplate> customTemplates =
                customDOs.stream().map(this::convertToTemplate).collect(Collectors.toList());
        return new SemanticTemplateListResp(builtinTemplates, customTemplates);
    }

    @Override
    public SemanticTemplate getTemplateById(Long id, User user) {
        SemanticTemplateDO templateDO = baseMapper.selectById(id);
        if (templateDO == null) {
            throw new InvalidArgumentException("Template not found: " + id);
        }
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
        template.setStatus(STATUS_DRAFT);

        SemanticTemplateDO templateDO = convertToDO(template);
        baseMapper.insert(templateDO);
        template.setId(templateDO.getId());
        return template;
    }

    @Override
    @Transactional
    public SemanticTemplate updateTemplate(SemanticTemplate template, User user) {
        SemanticTemplateDO existingDO = baseMapper.selectById(template.getId());
        if (existingDO == null) {
            throw new InvalidArgumentException("Template not found: " + template.getId());
        }

        Long tenantId = getTenantId(user);
        if (existingDO.getIsBuiltin() == 1) {
            if (!user.isSuperAdmin()) {
                throw new InvalidArgumentException("Only SaaS admin can update builtin templates");
            }
        } else {
            if (!existingDO.getTenantId().equals(tenantId)) {
                throw new InvalidArgumentException("No permission to update this template");
            }
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
        templateDO.setStatus(existingDO.getStatus());
        baseMapper.updateById(templateDO);
        return convertToTemplate(templateDO);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id, User user) {
        SemanticTemplateDO templateDO = baseMapper.selectById(id);
        if (templateDO == null) {
            return;
        }

        Long tenantId = getTenantId(user);
        if (templateDO.getIsBuiltin() == 1) {
            throw new InvalidArgumentException("Cannot delete builtin templates");
        }
        if (!templateDO.getTenantId().equals(tenantId)) {
            throw new InvalidArgumentException("No permission to delete this template");
        }
        if (!STATUS_DRAFT.equals(templateDO.getStatus())) {
            throw new InvalidArgumentException("Cannot delete template that has been deployed");
        }

        baseMapper.deleteById(id);
    }

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

        if (hasSuccessfulDeployment(templateId, tenantId)) {
            throw new InvalidArgumentException("该模板已成功部署过，不能重复部署。如需重新部署，请先删除之前部署创建的语义对象。");
        }

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

        try {
            deployment.setStatus(SemanticDeployment.DeploymentStatus.RUNNING);
            deployment.setStartTime(new Date());
            updateDeploymentStatus(deployment);

            SemanticDeployResult result = deployExecutor.execute(template, param, user);

            // Publish event for chat module to create Agent/Plugin
            applicationEventPublisher.publishEvent(
                    new TemplateDeployedEvent(this, result, template.getTemplateConfig(), user));

            deployment.setResultDetail(result);
            deployment.setStatus(SemanticDeployment.DeploymentStatus.SUCCESS);
            deployment.setEndTime(new Date());

            SemanticTemplateDO templateDO = baseMapper.selectById(templateId);
            if (templateDO != null && templateDO.getIsBuiltin() == 0) {
                templateDO.setStatus(STATUS_DEPLOYED);
                templateDO.setUpdatedAt(new Date());
                templateDO.setUpdatedBy(user.getName());
                baseMapper.updateById(templateDO);
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

    @Override
    public List<SemanticTemplate> getBuiltinTemplates() {
        QueryWrapper<SemanticTemplateDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(SemanticTemplateDO::getIsBuiltin, 1);
        List<SemanticTemplateDO> templateDOs = baseMapper.selectList(wrapper);
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

        template.setTenantId(tenantConfig.getDefaultTenantId());
        template.setIsBuiltin(true);
        template.setStatus(STATUS_DEPLOYED);

        QueryWrapper<SemanticTemplateDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(SemanticTemplateDO::getTenantId, tenantConfig.getDefaultTenantId())
                .eq(SemanticTemplateDO::getBizName, template.getBizName());
        SemanticTemplateDO existingDO = baseMapper.selectOne(wrapper);

        if (existingDO != null) {
            template.setId(existingDO.getId());
            template.setUpdatedBy(user.getName());
            template.setUpdatedAt(new Date());
            SemanticTemplateDO templateDO = convertToDO(template);
            baseMapper.updateById(templateDO);
        } else {
            template.setCreatedBy(user.getName());
            template.setCreatedAt(new Date());
            SemanticTemplateDO templateDO = convertToDO(template);
            baseMapper.insert(templateDO);
            template.setId(templateDO.getId());
        }

        return template;
    }

    // ============ Helper Methods ============

    private Long getTenantId(User user) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        if (user.getTenantId() != null) {
            return user.getTenantId();
        }
        return tenantConfig.getDefaultTenantId();
    }

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
        BeanUtils.copyProperties(templateDO, template, "isBuiltin", "templateConfig");
        template.setIsBuiltin(templateDO.getIsBuiltin() != null && templateDO.getIsBuiltin() == 1);
        if (StringUtils.isNotBlank(templateDO.getTemplateConfig())) {
            template.setTemplateConfig(JsonUtil.toObject(templateDO.getTemplateConfig(),
                    SemanticTemplateConfig.class));
        }
        return template;
    }

    private SemanticTemplateDO convertToDO(SemanticTemplate template) {
        SemanticTemplateDO templateDO = new SemanticTemplateDO();
        BeanUtils.copyProperties(template, templateDO, "isBuiltin", "templateConfig");
        templateDO.setIsBuiltin(template.getIsBuiltin() != null && template.getIsBuiltin() ? 1 : 0);
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
        BeanUtils.copyProperties(deploymentDO, deployment, "status", "paramConfig", "resultDetail");
        deployment.setStatus(SemanticDeployment.DeploymentStatus.valueOf(deploymentDO.getStatus()));
        if (StringUtils.isNotBlank(deploymentDO.getParamConfig())) {
            deployment.setParamConfig(
                    JsonUtil.toObject(deploymentDO.getParamConfig(), SemanticDeployParam.class));
        }
        if (StringUtils.isNotBlank(deploymentDO.getResultDetail())) {
            deployment.setResultDetail(
                    JsonUtil.toObject(deploymentDO.getResultDetail(), SemanticDeployResult.class));
        }
        return deployment;
    }

    private SemanticDeploymentDO convertToDeploymentDO(SemanticDeployment deployment) {
        SemanticDeploymentDO deploymentDO = new SemanticDeploymentDO();
        BeanUtils.copyProperties(deployment, deploymentDO, "status", "paramConfig", "resultDetail");
        deploymentDO.setStatus(deployment.getStatus().name());
        if (deployment.getParamConfig() != null) {
            deploymentDO.setParamConfig(JsonUtil.toString(deployment.getParamConfig()));
        }
        if (deployment.getResultDetail() != null) {
            deploymentDO.setResultDetail(JsonUtil.toString(deployment.getResultDetail()));
        }
        return deploymentDO;
    }
}
