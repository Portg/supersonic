package com.tencent.supersonic.auth.authentication.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.Permission;
import com.tencent.supersonic.auth.api.authentication.service.PermissionService;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/permission")
@Slf4j
public class PermissionController {

    private final PermissionService permissionService;
    private final UserService userService;

    public PermissionController(PermissionService permissionService, UserService userService) {
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @GetMapping("/list")
    public List<Permission> getAllPermissions() {
        return permissionService.getAllPermissions();
    }

    @GetMapping("/type/{type}")
    public List<Permission> getPermissionsByType(@PathVariable String type) {
        return permissionService.getPermissionsByType(type);
    }

    @GetMapping("/scope/{scope}")
    public List<Permission> getPermissionsByScope(@PathVariable String scope) {
        return permissionService.getPermissionsByScope(scope);
    }

    @GetMapping("/{id}")
    public Permission getPermissionById(@PathVariable Long id) {
        return permissionService.getPermissionById(id);
    }

    @GetMapping("/code/{code}")
    public Permission getPermissionByCode(@PathVariable String code) {
        return permissionService.getPermissionByCode(code);
    }

    @GetMapping("/tree")
    public List<Permission> getPermissionTree() {
        return permissionService.getPermissionTree();
    }

    @GetMapping("/current")
    public List<String> getCurrentUserPermissions(HttpServletRequest request,
            HttpServletResponse response) {
        User user = userService.getCurrentUser(request, response);
        if (user == null) {
            return List.of();
        }
        // 管理员返回所有权限
        if (user.getIsAdmin() != null && user.getIsAdmin() == 1) {
            return permissionService.getAllPermissions().stream().map(Permission::getCode).toList();
        }
        return permissionService.getPermissionCodesByUserId(user.getId());
    }

    @PostMapping
    public Permission createPermission(@RequestBody Permission permission,
            HttpServletRequest request, HttpServletResponse response)
            throws IllegalAccessException {
        User user = userService.getCurrentUser(request, response);
        checkAdminPermission(user);
        return permissionService.createPermission(permission);
    }

    @PutMapping
    public Permission updatePermission(@RequestBody Permission permission,
            HttpServletRequest request, HttpServletResponse response)
            throws IllegalAccessException {
        User user = userService.getCurrentUser(request, response);
        checkAdminPermission(user);
        return permissionService.updatePermission(permission);
    }

    @DeleteMapping("/{id}")
    public void deletePermission(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response) throws IllegalAccessException {
        User user = userService.getCurrentUser(request, response);
        checkAdminPermission(user);
        permissionService.deletePermission(id);
    }

    private void checkAdminPermission(User user) throws IllegalAccessException {
        if (user == null || user.getIsAdmin() != 1) {
            throw new IllegalAccessException("只有管理员才能执行此操作");
        }
    }
}
