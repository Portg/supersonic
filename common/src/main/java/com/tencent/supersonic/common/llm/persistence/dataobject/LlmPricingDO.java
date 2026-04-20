package com.tencent.supersonic.common.llm.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

@Data
@TableName("s2_llm_pricing")
public class LlmPricingDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String provider;
    private String model;
    private Long inputPricePer1kMicros;
    private Long outputPricePer1kMicros;
    private String currency;
    private Timestamp effectiveFrom;
    private Timestamp effectiveTo;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
