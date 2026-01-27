package com.tencent.supersonic.common.pojo.enums;


public enum TypeEnums {
    METRIC("指标"),
    DIMENSION("维度"),
    VALUE("值"),
    TAG("标签"),
    DOMAIN("领域"),
    DATASET("数据集"),
    MODEL("模型"),
    UNKNOWN("未知");

    private final String description;

    TypeEnums(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
