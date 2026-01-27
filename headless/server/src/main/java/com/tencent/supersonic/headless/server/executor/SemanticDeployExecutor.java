package com.tencent.supersonic.headless.server.executor;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.JoinCondition;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.headless.api.pojo.DataSetDetail;
import com.tencent.supersonic.headless.api.pojo.DataSetModelConfig;
import com.tencent.supersonic.headless.api.pojo.Dimension;
import com.tencent.supersonic.headless.api.pojo.DimensionTimeTypeParams;
import com.tencent.supersonic.headless.api.pojo.Field;
import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import com.tencent.supersonic.headless.api.pojo.ModelDetail;
import com.tencent.supersonic.headless.api.pojo.enums.DimensionType;
import com.tencent.supersonic.headless.api.pojo.enums.IdentifyType;
import com.tencent.supersonic.headless.api.pojo.request.DataSetReq;
import com.tencent.supersonic.headless.api.pojo.request.DomainReq;
import com.tencent.supersonic.headless.api.pojo.request.ModelReq;
import com.tencent.supersonic.headless.api.pojo.request.TermReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.server.pojo.SemanticDeployParam;
import com.tencent.supersonic.headless.server.pojo.SemanticDeployResult;
import com.tencent.supersonic.headless.server.pojo.SemanticPreviewResult;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplate;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.ConfigParam;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.DataSetConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.DimensionConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.DomainConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.IdentifyConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.MeasureConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.ModelConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.ModelRelationConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.TermConfig;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.DomainService;
import com.tencent.supersonic.headless.server.service.MetricService;
import com.tencent.supersonic.headless.server.service.ModelRelaService;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.service.TermService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SemanticDeployExecutor {

    @Autowired
    private DomainService domainService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private ModelRelaService modelRelaService;

    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private MetricService metricService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private TermService termService;

    /**
     * Preview deployment - returns what will be created without actually creating
     */
    public SemanticPreviewResult preview(SemanticTemplate template, SemanticDeployParam param,
            User user) {
        SemanticPreviewResult result = new SemanticPreviewResult();
        SemanticTemplateConfig config = template.getTemplateConfig();

        if (config == null) {
            throw new RuntimeException("Template config is null");
        }

        // Preview domain
        if (config.getDomain() != null) {
            DomainResp domainPreview = new DomainResp();
            domainPreview.setName(resolveParam(config.getDomain().getName(), param));
            domainPreview.setBizName(resolveParam(config.getDomain().getBizName(), param));
            domainPreview.setDescription(config.getDomain().getDescription());
            result.setDomain(domainPreview);
        }

        // Preview models
        if (!CollectionUtils.isEmpty(config.getModels())) {
            for (ModelConfig modelConfig : config.getModels()) {
                ModelResp modelPreview = new ModelResp();
                modelPreview.setName(resolveParam(modelConfig.getName(), param));
                modelPreview.setBizName(resolveParam(modelConfig.getBizName(), param));
                modelPreview.setDescription(modelConfig.getDescription());
                result.getModels().add(modelPreview);
            }
        }

        // Preview agent
        if (config.getAgent() != null) {
            SemanticPreviewResult.AgentPreview agentPreview =
                    new SemanticPreviewResult.AgentPreview();
            agentPreview.setName(resolveParam(config.getAgent().getName(), param));
            agentPreview.setDescription(config.getAgent().getDescription());
            agentPreview.setExamples(config.getAgent().getExamples());
            result.setAgent(agentPreview);
        }

        // Preview terms
        if (!CollectionUtils.isEmpty(config.getTerms())) {
            for (TermConfig termConfig : config.getTerms()) {
                SemanticPreviewResult.TermPreview termPreview =
                        new SemanticPreviewResult.TermPreview();
                termPreview.setName(termConfig.getName());
                termPreview.setDescription(termConfig.getDescription());
                termPreview.setAlias(termConfig.getAlias());
                result.getTerms().add(termPreview);
            }
        }

        return result;
    }

    /**
     * Execute deployment - actually creates all the semantic objects
     */
    public SemanticDeployResult execute(SemanticTemplate template, SemanticDeployParam param,
            User user) {
        SemanticDeployResult result = new SemanticDeployResult();
        SemanticTemplateConfig config = template.getTemplateConfig();

        if (config == null) {
            throw new RuntimeException("Template config is null");
        }

        try {
            // 1. Create Domain
            DomainResp domain = createDomain(config.getDomain(), param, user);
            result.setDomainId(domain.getId());
            result.setDomainName(domain.getName());

            // 2. Create Models
            Map<String, ModelResp> modelMap = new HashMap<>();
            if (!CollectionUtils.isEmpty(config.getModels())) {
                for (ModelConfig modelConfig : config.getModels()) {
                    ModelResp model =
                            createModel(modelConfig, domain, param.getDatabaseId(), param, user);
                    modelMap.put(modelConfig.getBizName(), model);

                    SemanticDeployResult.CreatedModel createdModel =
                            new SemanticDeployResult.CreatedModel();
                    createdModel.setId(model.getId());
                    createdModel.setName(model.getName());
                    createdModel.setBizName(model.getBizName());
                    result.getModels().add(createdModel);
                }
            }

            // 3. Create Model Relations
            if (!CollectionUtils.isEmpty(config.getModelRelations())) {
                for (ModelRelationConfig relaConfig : config.getModelRelations()) {
                    ModelResp fromModel = modelMap.get(relaConfig.getFromModelBizName());
                    ModelResp toModel = modelMap.get(relaConfig.getToModelBizName());
                    if (fromModel != null && toModel != null) {
                        createModelRelation(relaConfig, domain, fromModel, toModel, user);
                    }
                }
            }

            // 4. Collect metrics and dimensions for the result
            collectMetricsAndDimensions(domain.getId(), result);

            // 5. Create DataSet
            DataSetResp dataSet = createDataSet(config.getDataSet(), domain, param, user);
            result.setDataSetId(dataSet.getId());
            result.setDataSetName(dataSet.getName());

            // 6. Create Terms
            if (!CollectionUtils.isEmpty(config.getTerms())) {
                for (TermConfig termConfig : config.getTerms()) {
                    createTerm(termConfig, domain, user);

                    SemanticDeployResult.CreatedTerm createdTerm =
                            new SemanticDeployResult.CreatedTerm();
                    createdTerm.setName(termConfig.getName());
                    result.getTerms().add(createdTerm);
                }
            }

            // 7. Store AgentConfig for later creation through chat module
            // Agent is not created here to keep headless module independent from chat module
            if (config.getAgent() != null) {
                SemanticDeployResult.AgentConfigResult agentConfigResult =
                        new SemanticDeployResult.AgentConfigResult();
                agentConfigResult.setName(resolveParam(config.getAgent().getName(), param));
                agentConfigResult.setDescription(config.getAgent().getDescription());
                agentConfigResult.setEnableSearch(config.getAgent().getEnableSearch());
                agentConfigResult.setExamples(config.getAgent().getExamples());
                agentConfigResult.setDataSetId(dataSet.getId());
                agentConfigResult.setDataSetName(dataSet.getName());
                result.setAgentConfig(agentConfigResult);
            }

            log.info("Successfully deployed template: {} to domain: {}", template.getName(),
                    domain.getName());

        } catch (Exception e) {
            log.error("Failed to deploy template: {}", template.getName(), e);
            throw new RuntimeException("Failed to deploy template: " + e.getMessage(), e);
        }

        return result;
    }

    private DomainResp createDomain(DomainConfig config, SemanticDeployParam param, User user) {
        DomainReq domainReq = new DomainReq();
        domainReq.setName(resolveParam(config.getName(), param));
        domainReq.setBizName(resolveParam(config.getBizName(), param));
        domainReq.setDescription(config.getDescription());
        domainReq.setParentId(0L);
        domainReq.setStatus(StatusEnum.ONLINE.getCode());
        domainReq.setViewers(
                CollectionUtils.isEmpty(config.getViewers()) ? Lists.newArrayList(user.getName())
                        : config.getViewers());
        domainReq.setAdmins(
                CollectionUtils.isEmpty(config.getAdmins()) ? Lists.newArrayList(user.getName())
                        : config.getAdmins());
        domainReq.setIsOpen(config.getIsOpen() != null ? config.getIsOpen() : 0);
        return domainService.createDomain(domainReq, user);
    }

    private ModelResp createModel(ModelConfig config, DomainResp domain, Long databaseId,
            SemanticDeployParam param, User user) throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName(resolveParam(config.getName(), param));
        modelReq.setBizName(resolveParam(config.getBizName(), param));
        modelReq.setDescription(config.getDescription());
        modelReq.setDatabaseId(databaseId);
        modelReq.setDomainId(domain.getId());
        modelReq.setViewers(
                CollectionUtils.isEmpty(config.getViewers()) ? Lists.newArrayList(user.getName())
                        : config.getViewers());
        modelReq.setAdmins(
                CollectionUtils.isEmpty(config.getAdmins()) ? Lists.newArrayList(user.getName())
                        : config.getAdmins());

        ModelDetail modelDetail = new ModelDetail();

        // Identifiers
        List<Identify> identifiers = new ArrayList<>();
        if (!CollectionUtils.isEmpty(config.getIdentifiers())) {
            for (IdentifyConfig identifyConfig : config.getIdentifiers()) {
                IdentifyType identifyType =
                        "primary".equalsIgnoreCase(identifyConfig.getType()) ? IdentifyType.primary
                                : IdentifyType.foreign;
                identifiers.add(new Identify(identifyConfig.getName(), identifyType.name(),
                        resolveParam(identifyConfig.getFieldName(), param), 1));
            }
        }
        modelDetail.setIdentifiers(identifiers);

        // Dimensions
        List<Dimension> dimensions = new ArrayList<>();
        if (!CollectionUtils.isEmpty(config.getDimensions())) {
            for (DimensionConfig dimConfig : config.getDimensions()) {
                DimensionType dimType = parseDimensionType(dimConfig.getType());
                Dimension dimension =
                        new Dimension(dimConfig.getName(), dimConfig.getBizName(), dimType, 1);
                if (StringUtils.isNotBlank(dimConfig.getExpr())) {
                    dimension.setExpr(resolveParam(dimConfig.getExpr(), param));
                }
                if (dimType == DimensionType.partition_time || dimType == DimensionType.time) {
                    dimension.setTypeParams(new DimensionTimeTypeParams());
                }
                dimensions.add(dimension);
            }
        }
        modelDetail.setDimensions(dimensions);

        // Measures
        List<Measure> measures = new ArrayList<>();
        if (!CollectionUtils.isEmpty(config.getMeasures())) {
            for (MeasureConfig measureConfig : config.getMeasures()) {
                Measure measure = new Measure(measureConfig.getName(), measureConfig.getBizName(),
                        measureConfig.getAggOperator(), 1);
                if (StringUtils.isNotBlank(measureConfig.getExpr())) {
                    measure.setExpr(resolveParam(measureConfig.getExpr(), param));
                }
                if (StringUtils.isNotBlank(measureConfig.getConstraint())) {
                    measure.setConstraint(measureConfig.getConstraint());
                }
                if (measureConfig.getCreateMetric() != null) {
                    measure.setIsCreateMetric(measureConfig.getCreateMetric() ? 1 : 0);
                }
                measures.add(measure);
            }
        }
        modelDetail.setMeasures(measures);

        // Fields (auto-generated from identifiers, dimensions, and measures)
        List<Field> fields = new ArrayList<>();
        for (Identify identify : identifiers) {
            fields.add(
                    Field.builder().fieldName(identify.getBizName()).dataType("Varchar").build());
        }
        for (Dimension dimension : dimensions) {
            String dataType = dimension.getType() == DimensionType.time
                    || dimension.getType() == DimensionType.partition_time ? "Date" : "Varchar";
            fields.add(
                    Field.builder().fieldName(dimension.getBizName()).dataType(dataType).build());
        }
        for (Measure measure : measures) {
            fields.add(Field.builder().fieldName(measure.getBizName()).dataType("Double").build());
        }
        modelDetail.setFields(fields);

        // SQL Query
        String tableName = resolveParam(config.getTableName(), param);
        if (StringUtils.isNotBlank(config.getSqlQuery())) {
            modelDetail.setSqlQuery(resolveParam(config.getSqlQuery(), param));
        } else if (StringUtils.isNotBlank(tableName)) {
            modelDetail.setSqlQuery("SELECT * FROM " + tableName);
        }
        modelDetail.setQueryType("sql_query");

        modelReq.setModelDetail(modelDetail);
        return modelService.createModel(modelReq, user);
    }

    private void createModelRelation(ModelRelationConfig config, DomainResp domain,
            ModelResp fromModel, ModelResp toModel, User user) {
        List<JoinCondition> joinConditions = new ArrayList<>();
        if (!CollectionUtils.isEmpty(config.getJoinConditions())) {
            for (SemanticTemplateConfig.JoinCondition jc : config.getJoinConditions()) {
                FilterOperatorEnum operator = FilterOperatorEnum.EQUALS;
                if (StringUtils.isNotBlank(jc.getOperator())) {
                    try {
                        operator = FilterOperatorEnum.valueOf(jc.getOperator().toUpperCase());
                    } catch (Exception e) {
                        // Default to EQUALS
                    }
                }
                joinConditions
                        .add(new JoinCondition(jc.getLeftField(), jc.getRightField(), operator));
            }
        }

        ModelRela modelRela = new ModelRela();
        modelRela.setDomainId(domain.getId());
        modelRela.setFromModelId(fromModel.getId());
        modelRela.setToModelId(toModel.getId());
        modelRela.setJoinType(
                StringUtils.isNotBlank(config.getJoinType()) ? config.getJoinType() : "left join");
        modelRela.setJoinConditions(joinConditions);
        modelRelaService.save(modelRela, user);
    }

    private void collectMetricsAndDimensions(Long domainId, SemanticDeployResult result) {
        // Get all models in the domain
        List<ModelResp> models = modelService.getModelByDomainIds(Lists.newArrayList(domainId));

        for (ModelResp model : models) {
            MetaFilter metaFilter = new MetaFilter();
            metaFilter.setModelIds(Lists.newArrayList(model.getId()));

            // Collect metrics
            List<MetricResp> metrics = metricService.getMetrics(metaFilter);
            for (MetricResp metric : metrics) {
                SemanticDeployResult.CreatedMetric createdMetric =
                        new SemanticDeployResult.CreatedMetric();
                createdMetric.setId(metric.getId());
                createdMetric.setName(metric.getName());
                createdMetric.setBizName(metric.getBizName());
                createdMetric.setModelId(model.getId());
                result.getMetrics().add(createdMetric);
            }

            // Collect dimensions
            List<DimensionResp> dimensions = dimensionService.getDimensions(metaFilter);
            for (DimensionResp dimension : dimensions) {
                SemanticDeployResult.CreatedDimension createdDimension =
                        new SemanticDeployResult.CreatedDimension();
                createdDimension.setId(dimension.getId());
                createdDimension.setName(dimension.getName());
                createdDimension.setBizName(dimension.getBizName());
                createdDimension.setModelId(model.getId());
                result.getDimensions().add(createdDimension);
            }
        }
    }

    private DataSetResp createDataSet(DataSetConfig config, DomainResp domain,
            SemanticDeployParam param, User user) {
        DataSetReq dataSetReq = new DataSetReq();

        if (config != null) {
            dataSetReq.setName(resolveParam(config.getName(), param));
            dataSetReq.setBizName(resolveParam(config.getBizName(), param));
            dataSetReq.setDescription(config.getDescription());
            dataSetReq.setAdmins(
                    CollectionUtils.isEmpty(config.getAdmins()) ? Lists.newArrayList(user.getName())
                            : config.getAdmins());
        } else {
            // Default data set config
            dataSetReq.setName(domain.getName() + "数据集");
            dataSetReq.setBizName(domain.getBizName() + "_dataset");
            dataSetReq.setDescription("Auto-generated data set for " + domain.getName());
            dataSetReq.setAdmins(Lists.newArrayList(user.getName()));
        }

        dataSetReq.setDomainId(domain.getId());

        // Get all models and their metrics/dimensions
        List<DataSetModelConfig> dataSetModelConfigs = getDataSetModelConfigs(domain.getId());
        DataSetDetail dataSetDetail = new DataSetDetail();
        dataSetDetail.setDataSetModelConfigs(dataSetModelConfigs);
        dataSetReq.setDataSetDetail(dataSetDetail);
        dataSetReq.setTypeEnum(TypeEnums.DATASET);

        return dataSetService.save(dataSetReq, user);
    }

    private List<DataSetModelConfig> getDataSetModelConfigs(Long domainId) {
        List<DataSetModelConfig> dataSetModelConfigs = Lists.newArrayList();
        List<ModelResp> models = modelService.getModelByDomainIds(Lists.newArrayList(domainId));

        for (ModelResp model : models) {
            DataSetModelConfig dataSetModelConfig = new DataSetModelConfig();
            dataSetModelConfig.setId(model.getId());

            MetaFilter metaFilter = new MetaFilter();
            metaFilter.setModelIds(Lists.newArrayList(model.getId()));

            List<Long> metricIds = metricService.getMetrics(metaFilter).stream()
                    .map(MetricResp::getId).collect(Collectors.toList());
            dataSetModelConfig.setMetrics(metricIds);

            List<Long> dimensionIds = dimensionService.getDimensions(metaFilter).stream()
                    .map(DimensionResp::getId).collect(Collectors.toList());
            dataSetModelConfig.setDimensions(dimensionIds);

            dataSetModelConfigs.add(dataSetModelConfig);
        }

        return dataSetModelConfigs;
    }

    private void createTerm(TermConfig config, DomainResp domain, User user) {
        TermReq termReq = new TermReq();
        termReq.setName(config.getName());
        termReq.setDescription(config.getDescription());
        termReq.setAlias(config.getAlias());
        termReq.setDomainId(domain.getId());
        termService.saveOrUpdate(termReq, user);
    }

    private DimensionType parseDimensionType(String type) {
        if (StringUtils.isBlank(type)) {
            return DimensionType.categorical;
        }
        return switch (type.toLowerCase()) {
            case "time" -> DimensionType.time;
            case "partition_time" -> DimensionType.partition_time;
            default -> DimensionType.categorical;
        };
    }

    /**
     * Resolve parameter placeholders in the format ${paramKey}
     */
    private String resolveParam(String value, SemanticDeployParam param) {
        if (StringUtils.isBlank(value) || param == null || param.getParams() == null) {
            return value;
        }

        String result = value;
        for (Map.Entry<String, String> entry : param.getParams().entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, entry.getValue());
            }
        }
        return result;
    }
}
