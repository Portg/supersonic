package com.tencent.supersonic.auth.authentication.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.Tenant;
import com.tencent.supersonic.auth.api.authentication.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for admin tenant management. Requires super admin privileges.
 */
@RestController
@RequestMapping("/api/auth/admin/tenant")
@Slf4j
public class AdminTenantController {

    private final TenantService tenantService;

    public AdminTenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * Create a new tenant.
     */
    @PostMapping
    public ResponseEntity<Tenant> createTenant(@RequestBody Tenant tenant) {
        Tenant created = tenantService.createTenant(tenant);
        return ResponseEntity.ok(created);
    }

    /**
     * Get all tenants.
     */
    @GetMapping
    public ResponseEntity<List<Tenant>> getAllTenants() {
        List<Tenant> tenants = tenantService.getAllTenants();
        return ResponseEntity.ok(tenants);
    }

    /**
     * Get active tenants only.
     */
    @GetMapping("/active")
    public ResponseEntity<List<Tenant>> getActiveTenants() {
        List<Tenant> tenants = tenantService.getActiveTenants();
        return ResponseEntity.ok(tenants);
    }

    /**
     * Get a tenant by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Tenant> getTenant(@PathVariable Long id) {
        return tenantService.getTenantById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get a tenant by code.
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<Tenant> getTenantByCode(@PathVariable String code) {
        return tenantService.getTenantByCode(code).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a tenant.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Tenant> updateTenant(@PathVariable Long id, @RequestBody Tenant tenant) {
        tenant.setId(id);
        Tenant updated = tenantService.updateTenant(tenant);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a tenant (soft delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTenant(@PathVariable Long id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Suspend a tenant.
     */
    @PostMapping("/{id}/suspend")
    public ResponseEntity<Void> suspendTenant(@PathVariable Long id) {
        tenantService.suspendTenant(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Activate a tenant.
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateTenant(@PathVariable Long id) {
        tenantService.activateTenant(id);
        return ResponseEntity.ok().build();
    }
}
