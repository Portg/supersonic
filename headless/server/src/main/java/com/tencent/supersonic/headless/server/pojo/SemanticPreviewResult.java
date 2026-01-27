package com.tencent.supersonic.headless.server.pojo;

import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SemanticPreviewResult {

    private DomainResp domain;

    private List<ModelResp> models = new ArrayList<>();

    private List<MetricResp> metrics = new ArrayList<>();

    private List<DimensionResp> dimensions = new ArrayList<>();

    private DataSetResp dataSet;

    private AgentPreview agent;

    private List<TermPreview> terms = new ArrayList<>();

    @Data
    public static class AgentPreview {
        private String name;
        private String description;
        private List<String> examples = new ArrayList<>();
    }

    @Data
    public static class TermPreview {
        private String name;
        private String description;
        private List<String> alias = new ArrayList<>();
    }
}
