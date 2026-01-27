package com.tencent.supersonic.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements Serializable {

    private Long id;

    private String name;

    private String displayName;

    private String email;

    private Integer isAdmin;

    private Timestamp lastLogin;

    /** Tenant ID for multi-tenancy */
    private Long tenantId;

    /** User role within tenant */
    private String role;

    /** User role ID */
    private Long roleId;

    /** User permissions (for menu control) */
    private List<String> permissions;

    /** User status: 1=enabled, 0=disabled */
    private Integer status;

    /** Primary organization ID */
    private Long organizationId;

    /** Primary organization name */
    private String organizationName;

    /** Role IDs */
    private List<Long> roleIds;

    /** Role names */
    private List<String> roleNames;

    public static User get(Long id, String name, String displayName, String email,
            Integer isAdmin) {
        return User.builder().id(id).name(name).displayName(displayName).email(email)
                .isAdmin(isAdmin).tenantId(1L).role("USER").build();
    }

    public static User get(Long id, String name, String displayName, String email, Integer isAdmin,
            Long tenantId, String role) {
        return User.builder().id(id).name(name).displayName(displayName).email(email)
                .isAdmin(isAdmin).tenantId(tenantId).role(role).build();
    }

    public static User get(Long id, String name) {
        return User.builder().id(id).name(name).displayName(name).email(name).isAdmin(0)
                .tenantId(0L).role("VISITOR").build();
    }

    public static User getDefaultUser() {
        return User.builder().id(1L).name("admin").displayName("admin").email("admin@email")
                .isAdmin(1).tenantId(1L).role("ADMIN").build();
    }

    public static User getVisitUser() {
        return User.builder().id(1L).name("visit").displayName("visit").email("visit@email")
                .isAdmin(0).tenantId(0L).role("VISITOR").build();
    }

    public static User getAppUser(int appId) {
        String name = String.format("app_%s", appId);
        return User.builder().id(1L).name(name).displayName(name).email("").isAdmin(1).tenantId(1L)
                .role("APP").build();
    }

    public String getDisplayName() {
        return StringUtils.isBlank(displayName) ? name : displayName;
    }

    public boolean isSuperAdmin() {
        return isAdmin != null && isAdmin == 1;
    }
}
