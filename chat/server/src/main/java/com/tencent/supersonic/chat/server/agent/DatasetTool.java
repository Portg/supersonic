package com.tencent.supersonic.chat.server.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetTool extends AgentTool {

    private List<Long> dataSetIds;
    private List<String> exampleQuestions;
}
