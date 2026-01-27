package com.tencent.supersonic.auth.api.authentication.pojo;

import com.tencent.supersonic.common.pojo.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_CREATE_TIME;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_IS_ADMIN;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_TENANT_ID;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_DISPLAY_NAME;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_EMAIL;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_ID;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_NAME;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_PASSWORD;
import static com.tencent.supersonic.auth.api.authentication.constant.UserConstants.TOKEN_USER_ROLE;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserWithPassword extends User {

    private String password;

    public UserWithPassword() {
        super();
    }

    public UserWithPassword(Long id, String name, String displayName, String email, String password,
            Integer isAdmin) {
        super();
        this.setId(id);
        this.setName(name);
        this.setDisplayName(displayName);
        this.setEmail(email);
        this.setIsAdmin(isAdmin);
        this.password = password;
    }

    public UserWithPassword(Long id, String name, String displayName, String email, String password,
            Integer isAdmin, Long tenantId, String role) {
        this(id, name, displayName, email, password, isAdmin);
        this.setTenantId(tenantId);
        this.setRole(role);
    }

    public UserWithPassword(Long id, String name, String displayName, String email, String password,
            Integer isAdmin, Long tenantId, String role, Long roleId) {
        this(id, name, displayName, email, password, isAdmin, tenantId, role);
        this.setRoleId(roleId);
    }

    public static UserWithPassword get(Long id, String name, String displayName, String email,
            String password, Integer isAdmin) {
        return new UserWithPassword(id, name, displayName, email, password, isAdmin);
    }

    public static UserWithPassword get(Long id, String name, String displayName, String email,
            String password, Integer isAdmin, Long tenantId, String role) {
        return new UserWithPassword(id, name, displayName, email, password, isAdmin, tenantId,
                role);
    }

    public static UserWithPassword get(Long id, String name, String displayName, String email,
            String password, Integer isAdmin, Long tenantId, String role, Long roleId) {
        return new UserWithPassword(id, name, displayName, email, password, isAdmin, tenantId, role,
                roleId);
    }

    public static Map<String, Object> convert(UserWithPassword user) {
        Map<String, Object> claims = new HashMap<>(9);
        claims.put(TOKEN_USER_ID, user.getId());
        claims.put(TOKEN_USER_NAME, StringUtils.isEmpty(user.getName()) ? "" : user.getName());
        claims.put(TOKEN_USER_PASSWORD,
                StringUtils.isEmpty(user.getPassword()) ? "" : user.getPassword());
        claims.put(TOKEN_USER_EMAIL, StringUtils.isEmpty(user.getEmail()) ? "" : user.getEmail());
        claims.put(TOKEN_USER_DISPLAY_NAME, user.getDisplayName());
        claims.put(TOKEN_CREATE_TIME, System.currentTimeMillis());
        claims.put(TOKEN_IS_ADMIN, user.getIsAdmin());
        claims.put(TOKEN_TENANT_ID, user.getTenantId());
        claims.put(TOKEN_USER_ROLE, StringUtils.isEmpty(user.getRole()) ? "" : user.getRole());
        return claims;
    }
}
