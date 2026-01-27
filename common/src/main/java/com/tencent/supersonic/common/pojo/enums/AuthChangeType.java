package com.tencent.supersonic.common.pojo.enums;

/**
 * Types of authorization change operations.
 */
public enum AuthChangeType {
    CREATE("CREATE", "创建权限组"),
    UPDATE("UPDATE", "更新权限组"),
    DELETE("DELETE", "删除权限组"),
    GRANT("GRANT", "授权用户/部门"),
    REVOKE("REVOKE", "撤销授权");

    private final String code;
    private final String description;

    AuthChangeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
