package com.tencent.supersonic.headless.server.executor;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplate;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.AgentConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.ConfigParam;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.DataSetConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.DimensionConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.DomainConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.IdentifyConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.JoinCondition;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.MeasureConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.ModelConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.ModelRelationConfig;
import com.tencent.supersonic.headless.server.pojo.SemanticTemplateConfig.TermConfig;
import com.tencent.supersonic.headless.server.service.SemanticTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Initializes builtin semantic templates at application startup. Converts existing Demo patterns
 * into reusable templates.
 */
@Component
@Order(0) // Run before other demo initializers
@Slf4j
public class BuiltinSemanticTemplateInitializer implements CommandLineRunner {

    @Autowired
    private SemanticTemplateService semanticTemplateService;

    @Override
    public void run(String... args) {
        try {
            initBuiltinTemplates();
        } catch (Exception e) {
            log.error("Failed to initialize builtin templates", e);
        }
    }

    private void initBuiltinTemplates() {
        // Check if already initialized
        List<SemanticTemplate> existingTemplates = semanticTemplateService.getBuiltinTemplates();
        if (!existingTemplates.isEmpty()) {
            log.info("Builtin templates already exist, skipping initialization");
            return;
        }

        log.info("Initializing builtin semantic templates...");
        User adminUser = User.getDefaultUser();

        // Initialize Visits Template (based on S2VisitsDemo)
        initVisitsTemplate(adminUser);

        // Initialize Singer Template (based on S2SingerDemo)
        initSingerTemplate(adminUser);

        // Initialize Company Template (based on S2CompanyDemo)
        initCompanyTemplate(adminUser);

        log.info("Builtin semantic templates initialized successfully");
    }

    private void initVisitsTemplate(User user) {
        SemanticTemplate template = new SemanticTemplate();
        template.setName("访问统计模板");
        template.setBizName("visits_template");
        template.setCategory("VISITS");
        template.setDescription("用于分析产品访问情况，包含用户、PV/UV、停留时长等指标。适合产品运营分析场景。");
        template.setStatus(1);

        SemanticTemplateConfig config = new SemanticTemplateConfig();

        // Config params
        List<ConfigParam> params = new ArrayList<>();
        params.add(createParam("domain_name", "域名称", "TEXT", "产品数据域", true, "语义域名称"));
        params.add(createParam("domain_bizname", "域代码", "TEXT", "supersonic", true, "语义域代码"));
        params.add(
                createParam("table_user", "用户表", "TABLE", "s2_user_department", true, "用户部门信息表"));
        params.add(createParam("table_pv_uv", "PV/UV表", "TABLE", "s2_pv_uv_statis", true, "访问统计表"));
        params.add(createParam("table_stay", "停留时长表", "TABLE", "s2_stay_time_statis", true,
                "停留时长统计表"));
        config.setConfigParams(params);

        // Domain
        DomainConfig domain = new DomainConfig();
        domain.setName("${domain_name}");
        domain.setBizName("${domain_bizname}");
        domain.setDescription("产品访问数据域");
        domain.setIsOpen(1);
        config.setDomain(domain);

        // Model 1: User Department
        ModelConfig userModel = new ModelConfig();
        userModel.setName("用户部门");
        userModel.setBizName("user_department");
        userModel.setDescription("用户部门信息");
        userModel.setTableName("${table_user}");

        IdentifyConfig userIdentify = new IdentifyConfig();
        userIdentify.setName("用户名");
        userIdentify.setBizName("user_name");
        userIdentify.setFieldName("user_name");
        userIdentify.setType("primary");
        userModel.setIdentifiers(Collections.singletonList(userIdentify));

        DimensionConfig deptDim = new DimensionConfig();
        deptDim.setName("部门");
        deptDim.setBizName("department");
        deptDim.setFieldName("department");
        deptDim.setType("categorical");
        deptDim.setEnableDictValue(true);
        userModel.setDimensions(Collections.singletonList(deptDim));

        // Model 2: PV/UV Stats
        ModelConfig pvUvModel = new ModelConfig();
        pvUvModel.setName("PVUV统计");
        pvUvModel.setBizName("s2_pv_uv_statis");
        pvUvModel.setDescription("访问次数和用户数统计");
        pvUvModel.setTableName("${table_pv_uv}");

        IdentifyConfig pvIdentify = new IdentifyConfig();
        pvIdentify.setName("用户名");
        pvIdentify.setBizName("user_name");
        pvIdentify.setFieldName("user_name");
        pvIdentify.setType("foreign");
        pvUvModel.setIdentifiers(Collections.singletonList(pvIdentify));

        List<DimensionConfig> pvDims = new ArrayList<>();
        DimensionConfig dateDim = new DimensionConfig();
        dateDim.setName("数据日期");
        dateDim.setBizName("imp_date");
        dateDim.setFieldName("imp_date");
        dateDim.setType("partition_time");
        pvDims.add(dateDim);

        DimensionConfig pageDim = new DimensionConfig();
        pageDim.setName("页面");
        pageDim.setBizName("page");
        pageDim.setFieldName("page");
        pageDim.setType("categorical");
        pvDims.add(pageDim);
        pvUvModel.setDimensions(pvDims);

        // Model 3: Stay Time Stats
        ModelConfig stayModel = new ModelConfig();
        stayModel.setName("停留时长统计");
        stayModel.setBizName("s2_stay_time_statis");
        stayModel.setDescription("用户停留时长统计");
        stayModel.setTableName("${table_stay}");

        IdentifyConfig stayIdentify = new IdentifyConfig();
        stayIdentify.setName("用户名");
        stayIdentify.setBizName("user_name");
        stayIdentify.setFieldName("user_name");
        stayIdentify.setType("foreign");
        stayModel.setIdentifiers(Collections.singletonList(stayIdentify));

        List<DimensionConfig> stayDims = new ArrayList<>();
        DimensionConfig stayDateDim = new DimensionConfig();
        stayDateDim.setName("数据日期");
        stayDateDim.setBizName("imp_date");
        stayDateDim.setFieldName("imp_date");
        stayDateDim.setType("partition_time");
        stayDims.add(stayDateDim);

        DimensionConfig stayPageDim = new DimensionConfig();
        stayPageDim.setName("页面");
        stayPageDim.setBizName("visits_page");
        stayPageDim.setFieldName("page");
        stayPageDim.setType("categorical");
        stayPageDim.setExpr("page");
        stayDims.add(stayPageDim);
        stayModel.setDimensions(stayDims);

        MeasureConfig stayMeasure = new MeasureConfig();
        stayMeasure.setName("停留时长");
        stayMeasure.setBizName("stay_hours");
        stayMeasure.setFieldName("stay_hours");
        stayMeasure.setAggOperator(AggregateTypeEnum.SUM.name());
        stayMeasure.setCreateMetric(true);
        stayModel.setMeasures(Collections.singletonList(stayMeasure));

        config.setModels(Arrays.asList(userModel, pvUvModel, stayModel));

        // Model Relations
        List<ModelRelationConfig> relations = new ArrayList<>();
        relations.add(createRelation("s2_pv_uv_statis", "user_department", "user_name"));
        relations.add(createRelation("s2_stay_time_statis", "user_department", "user_name"));
        config.setModelRelations(relations);

        // DataSet
        DataSetConfig dataSet = new DataSetConfig();
        dataSet.setName("${domain_name}数据集");
        dataSet.setBizName("${domain_bizname}_dataset");
        dataSet.setDescription("包含访问统计相关的指标和维度");
        config.setDataSet(dataSet);

        // Agent
        AgentConfig agent = new AgentConfig();
        agent.setName("${domain_name}分析助手");
        agent.setDescription("帮助您分析产品的用户访问情况");
        agent.setEnableSearch(true);
        agent.setExamples(Arrays.asList("近15天访问次数汇总", "按部门统计访问人数", "过去30天访问次数最高的部门top3",
                "近1个月总访问次数超过100次的部门有几个"));
        config.setAgent(agent);

        // Terms
        List<TermConfig> terms = new ArrayList<>();
        TermConfig recentTerm = new TermConfig();
        recentTerm.setName("近期");
        recentTerm.setDescription("指近10天");
        recentTerm.setAlias(Collections.singletonList("近一段时间"));
        terms.add(recentTerm);

        TermConfig vipTerm = new TermConfig();
        vipTerm.setName("核心用户");
        vipTerm.setDescription("用户为tom和lucy");
        vipTerm.setAlias(Collections.singletonList("VIP用户"));
        terms.add(vipTerm);
        config.setTerms(terms);

        template.setTemplateConfig(config);
        semanticTemplateService.saveBuiltinTemplate(template, user);
        log.info("Initialized visits template");
    }

    private void initSingerTemplate(User user) {
        SemanticTemplate template = new SemanticTemplate();
        template.setName("歌手库模板");
        template.setBizName("singer_template");
        template.setCategory("SINGER");
        template.setDescription("用于分析歌手和歌曲数据。适合音乐、娱乐行业数据分析场景。");
        template.setStatus(1);

        SemanticTemplateConfig config = new SemanticTemplateConfig();

        // Config params
        List<ConfigParam> params = new ArrayList<>();
        params.add(createParam("domain_name", "域名称", "TEXT", "歌手数据域", true, "语义域名称"));
        params.add(createParam("domain_bizname", "域代码", "TEXT", "singer", true, "语义域代码"));
        params.add(createParam("table_singer", "歌手表", "TABLE", "singer", true, "歌手信息表"));
        config.setConfigParams(params);

        // Domain
        DomainConfig domain = new DomainConfig();
        domain.setName("${domain_name}");
        domain.setBizName("${domain_bizname}");
        domain.setDescription("歌手音乐数据域");
        domain.setIsOpen(1);
        config.setDomain(domain);

        // Model: Singer
        ModelConfig singerModel = new ModelConfig();
        singerModel.setName("歌手库");
        singerModel.setBizName("singer");
        singerModel.setDescription("歌手基本信息和歌曲数据");
        singerModel.setTableName("${table_singer}");

        IdentifyConfig singerIdentify = new IdentifyConfig();
        singerIdentify.setName("歌手名");
        singerIdentify.setBizName("singer_name");
        singerIdentify.setFieldName("singer_name");
        singerIdentify.setType("primary");
        singerModel.setIdentifiers(Collections.singletonList(singerIdentify));

        List<DimensionConfig> singerDims = new ArrayList<>();
        DimensionConfig actDateDim = new DimensionConfig();
        actDateDim.setName("活跃区间");
        actDateDim.setBizName("act_area");
        actDateDim.setFieldName("act_area");
        actDateDim.setType("categorical");
        singerDims.add(actDateDim);

        DimensionConfig genreDim = new DimensionConfig();
        genreDim.setName("流派");
        genreDim.setBizName("genre");
        genreDim.setFieldName("genre");
        genreDim.setType("categorical");
        genreDim.setEnableDictValue(true);
        singerDims.add(genreDim);
        singerModel.setDimensions(singerDims);

        List<MeasureConfig> singerMeasures = new ArrayList<>();
        MeasureConfig songMeasure = new MeasureConfig();
        songMeasure.setName("歌曲数");
        songMeasure.setBizName("song_count");
        songMeasure.setFieldName("song_count");
        songMeasure.setAggOperator(AggregateTypeEnum.SUM.name());
        songMeasure.setCreateMetric(true);
        singerMeasures.add(songMeasure);

        MeasureConfig playMeasure = new MeasureConfig();
        playMeasure.setName("播放量");
        playMeasure.setBizName("play_count");
        playMeasure.setFieldName("play_count");
        playMeasure.setAggOperator(AggregateTypeEnum.SUM.name());
        playMeasure.setCreateMetric(true);
        singerMeasures.add(playMeasure);
        singerModel.setMeasures(singerMeasures);

        config.setModels(Collections.singletonList(singerModel));

        // DataSet
        DataSetConfig dataSet = new DataSetConfig();
        dataSet.setName("${domain_name}数据集");
        dataSet.setBizName("${domain_bizname}_dataset");
        dataSet.setDescription("包含歌手相关的指标和维度");
        config.setDataSet(dataSet);

        // Agent
        AgentConfig agent = new AgentConfig();
        agent.setName("${domain_name}分析助手");
        agent.setDescription("帮助您分析歌手和歌曲数据");
        agent.setEnableSearch(true);
        agent.setExamples(Arrays.asList("歌曲数最多的歌手", "按流派统计歌手数量", "播放量最高的歌手top10", "周杰伦的歌曲数和播放量"));
        config.setAgent(agent);

        template.setTemplateConfig(config);
        semanticTemplateService.saveBuiltinTemplate(template, user);
        log.info("Initialized singer template");
    }

    private void initCompanyTemplate(User user) {
        SemanticTemplate template = new SemanticTemplate();
        template.setName("企业分析模板");
        template.setBizName("company_template");
        template.setCategory("COMPANY");
        template.setDescription("用于分析企业、品牌和收入数据。适合企业经营分析场景。");
        template.setStatus(1);

        SemanticTemplateConfig config = new SemanticTemplateConfig();

        // Config params
        List<ConfigParam> params = new ArrayList<>();
        params.add(createParam("domain_name", "域名称", "TEXT", "企业数据域", true, "语义域名称"));
        params.add(createParam("domain_bizname", "域代码", "TEXT", "company", true, "语义域代码"));
        params.add(createParam("table_company", "企业表", "TABLE", "company", true, "企业信息表"));
        params.add(createParam("table_brand", "品牌表", "TABLE", "brand", true, "品牌信息表"));
        params.add(createParam("table_revenue", "收入表", "TABLE", "brand_revenue", true, "品牌收入表"));
        config.setConfigParams(params);

        // Domain
        DomainConfig domain = new DomainConfig();
        domain.setName("${domain_name}");
        domain.setBizName("${domain_bizname}");
        domain.setDescription("企业经营数据域");
        domain.setIsOpen(1);
        config.setDomain(domain);

        // Model 1: Company
        ModelConfig companyModel = new ModelConfig();
        companyModel.setName("企业信息");
        companyModel.setBizName("company");
        companyModel.setDescription("企业基本信息");
        companyModel.setTableName("${table_company}");

        IdentifyConfig companyIdentify = new IdentifyConfig();
        companyIdentify.setName("公司名");
        companyIdentify.setBizName("company_name");
        companyIdentify.setFieldName("company_name");
        companyIdentify.setType("primary");
        companyModel.setIdentifiers(Collections.singletonList(companyIdentify));

        DimensionConfig industryDim = new DimensionConfig();
        industryDim.setName("行业");
        industryDim.setBizName("industry");
        industryDim.setFieldName("industry");
        industryDim.setType("categorical");
        industryDim.setEnableDictValue(true);
        companyModel.setDimensions(Collections.singletonList(industryDim));

        // Model 2: Brand
        ModelConfig brandModel = new ModelConfig();
        brandModel.setName("品牌信息");
        brandModel.setBizName("brand");
        brandModel.setDescription("品牌信息");
        brandModel.setTableName("${table_brand}");

        IdentifyConfig brandIdentify = new IdentifyConfig();
        brandIdentify.setName("品牌名");
        brandIdentify.setBizName("brand_name");
        brandIdentify.setFieldName("brand_name");
        brandIdentify.setType("primary");

        IdentifyConfig brandCompanyIdentify = new IdentifyConfig();
        brandCompanyIdentify.setName("公司名");
        brandCompanyIdentify.setBizName("company_name");
        brandCompanyIdentify.setFieldName("company_name");
        brandCompanyIdentify.setType("foreign");
        brandModel.setIdentifiers(Arrays.asList(brandIdentify, brandCompanyIdentify));

        DimensionConfig categoryDim = new DimensionConfig();
        categoryDim.setName("品类");
        categoryDim.setBizName("category");
        categoryDim.setFieldName("category");
        categoryDim.setType("categorical");
        brandModel.setDimensions(Collections.singletonList(categoryDim));

        // Model 3: Brand Revenue
        ModelConfig revenueModel = new ModelConfig();
        revenueModel.setName("品牌收入");
        revenueModel.setBizName("brand_revenue");
        revenueModel.setDescription("品牌收入数据");
        revenueModel.setTableName("${table_revenue}");

        IdentifyConfig revBrandIdentify = new IdentifyConfig();
        revBrandIdentify.setName("品牌名");
        revBrandIdentify.setBizName("brand_name");
        revBrandIdentify.setFieldName("brand_name");
        revBrandIdentify.setType("foreign");
        revenueModel.setIdentifiers(Collections.singletonList(revBrandIdentify));

        DimensionConfig yearDim = new DimensionConfig();
        yearDim.setName("年份");
        yearDim.setBizName("year");
        yearDim.setFieldName("year");
        yearDim.setType("partition_time");
        revenueModel.setDimensions(Collections.singletonList(yearDim));

        MeasureConfig revenueMeasure = new MeasureConfig();
        revenueMeasure.setName("收入金额");
        revenueMeasure.setBizName("revenue");
        revenueMeasure.setFieldName("revenue");
        revenueMeasure.setAggOperator(AggregateTypeEnum.SUM.name());
        revenueMeasure.setCreateMetric(true);
        revenueModel.setMeasures(Collections.singletonList(revenueMeasure));

        config.setModels(Arrays.asList(companyModel, brandModel, revenueModel));

        // Model Relations
        List<ModelRelationConfig> relations = new ArrayList<>();
        relations.add(createRelation("brand", "company", "company_name"));
        relations.add(createRelation("brand_revenue", "brand", "brand_name"));
        config.setModelRelations(relations);

        // DataSet
        DataSetConfig dataSet = new DataSetConfig();
        dataSet.setName("${domain_name}数据集");
        dataSet.setBizName("${domain_bizname}_dataset");
        dataSet.setDescription("包含企业经营相关的指标和维度");
        config.setDataSet(dataSet);

        // Agent
        AgentConfig agent = new AgentConfig();
        agent.setName("${domain_name}分析助手");
        agent.setDescription("帮助您分析企业经营数据");
        agent.setEnableSearch(true);
        agent.setExamples(Arrays.asList("各行业的企业数量", "收入最高的品牌top10", "腾讯旗下的品牌有哪些", "按年份统计总收入趋势"));
        config.setAgent(agent);

        template.setTemplateConfig(config);
        semanticTemplateService.saveBuiltinTemplate(template, user);
        log.info("Initialized company template");
    }

    private ConfigParam createParam(String key, String name, String type, String defaultValue,
            boolean required, String description) {
        ConfigParam param = new ConfigParam();
        param.setKey(key);
        param.setName(name);
        param.setType(type);
        param.setDefaultValue(defaultValue);
        param.setRequired(required);
        param.setDescription(description);
        return param;
    }

    private ModelRelationConfig createRelation(String fromBizName, String toBizName,
            String joinField) {
        ModelRelationConfig relation = new ModelRelationConfig();
        relation.setFromModelBizName(fromBizName);
        relation.setToModelBizName(toBizName);
        relation.setJoinType("left join");

        JoinCondition condition = new JoinCondition();
        condition.setLeftField(joinField);
        condition.setRightField(joinField);
        condition.setOperator("EQUALS");
        relation.setJoinConditions(Collections.singletonList(condition));

        return relation;
    }
}
