package com.tencent.supersonic.auth.authentication.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.Organization;
import com.tencent.supersonic.auth.api.authentication.request.OrganizationReq;
import com.tencent.supersonic.auth.api.authentication.request.UserOrganizationReq;
import com.tencent.supersonic.auth.api.authentication.service.OrganizationService;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/auth/organization")
@Slf4j
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserService userService;

    public OrganizationController(OrganizationService organizationService,
            UserService userService) {
        this.organizationService = organizationService;
        this.userService = userService;
    }

    /**
     * Get organization tree
     */
    @GetMapping("/tree")
    public List<Organization> getOrganizationTree() {
        return organizationService.getOrganizationTree();
    }

    /**
     * Get organization by id
     */
    @GetMapping("/{id}")
    public Organization getOrganization(@PathVariable("id") Long id) {
        return organizationService.getOrganization(id);
    }

    /**
     * Create organization
     */
    @PostMapping
    public Organization createOrganization(@RequestBody OrganizationReq req,
            HttpServletRequest request, HttpServletResponse response) {
        User currentUser = userService.getCurrentUser(request, response);
        return organizationService.createOrganization(req, currentUser.getName());
    }

    /**
     * Update organization
     */
    @PutMapping("/{id}")
    public Organization updateOrganization(@PathVariable("id") Long id,
            @RequestBody OrganizationReq req, HttpServletRequest request,
            HttpServletResponse response) {
        User currentUser = userService.getCurrentUser(request, response);
        return organizationService.updateOrganization(id, req, currentUser.getName());
    }

    /**
     * Delete organization
     */
    @DeleteMapping("/{id}")
    public void deleteOrganization(@PathVariable("id") Long id) {
        organizationService.deleteOrganization(id);
    }

    /**
     * Get users by organization
     */
    @GetMapping("/{id}/users")
    public List<Long> getUsersByOrganization(@PathVariable("id") Long id) {
        return organizationService.getUserIdsByOrganization(id);
    }

    /**
     * Get user's organizations
     */
    @GetMapping("/user/{userId}")
    public Set<Long> getUserOrganizations(@PathVariable("userId") Long userId) {
        return organizationService.getOrganizationIdsByUser(userId);
    }

    /**
     * Assign user to organization
     */
    @PostMapping("/assign")
    public void assignUserToOrganization(@RequestBody UserOrganizationReq req,
            HttpServletRequest request, HttpServletResponse response) {
        User currentUser = userService.getCurrentUser(request, response);
        boolean isPrimary = req.getIsPrimary() != null && req.getIsPrimary();
        organizationService.assignUserToOrganization(req.getUserId(), req.getOrganizationId(),
                isPrimary, currentUser.getName());
    }

    /**
     * Remove user from organization
     */
    @PostMapping("/remove")
    public void removeUserFromOrganization(@RequestBody UserOrganizationReq req) {
        organizationService.removeUserFromOrganization(req.getUserId(), req.getOrganizationId());
    }

    /**
     * Set user's primary organization
     */
    @PostMapping("/setPrimary")
    public void setUserPrimaryOrganization(@RequestBody UserOrganizationReq req) {
        organizationService.setUserPrimaryOrganization(req.getUserId(), req.getOrganizationId());
    }

    /**
     * Batch assign users to organization
     */
    @PostMapping("/batchAssign")
    public void batchAssignUsersToOrganization(@RequestBody UserOrganizationReq req,
            HttpServletRequest request, HttpServletResponse response) {
        User currentUser = userService.getCurrentUser(request, response);
        organizationService.batchAssignUsersToOrganization(req.getUserIds(),
                req.getOrganizationId(), currentUser.getName());
    }

    /**
     * Batch remove users from organization
     */
    @PostMapping("/batchRemove")
    public void batchRemoveUsersFromOrganization(@RequestBody UserOrganizationReq req) {
        organizationService.batchRemoveUsersFromOrganization(req.getUserIds(),
                req.getOrganizationId());
    }
}
