package com.tencent.supersonic.headless.server.rest;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import com.tencent.supersonic.common.llm.service.LlmUsageService;
import com.tencent.supersonic.common.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/semantic/admin/llm-usage")
@RequiredArgsConstructor
public class LlmUsageController {

    private final LlmUsageService llmUsageService;
    private final UserService userService;

    @GetMapping
    public IPage<LlmUsageDO> query(@RequestParam Long tenantId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String callType,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request, HttpServletResponse response)
            throws IllegalAccessException {
        checkAdminPermission(userService.getCurrentUser(request, response));
        return llmUsageService.query(tenantId, from, to, model, callType, page, size);
    }

    @GetMapping("/daily")
    public List<Map<String, Object>> daily(@RequestParam Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request, HttpServletResponse response)
            throws IllegalAccessException {
        checkAdminPermission(userService.getCurrentUser(request, response));
        return llmUsageService.dailyAggregates(tenantId, from, to);
    }

    @GetMapping("/total-tokens")
    public long totalTokens(@RequestParam Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request, HttpServletResponse response)
            throws IllegalAccessException {
        checkAdminPermission(userService.getCurrentUser(request, response));
        return llmUsageService.sumTokens(tenantId, from, to);
    }

    private void checkAdminPermission(User user) throws IllegalAccessException {
        if (user == null || user.getIsAdmin() == null || user.getIsAdmin() != 1) {
            throw new IllegalAccessException("只有管理员才能执行此操作");
        }
    }
}
