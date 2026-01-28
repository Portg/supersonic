package com.tencent.supersonic.common.service;

import com.tencent.supersonic.common.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@FunctionalInterface
public interface CurrentUserProvider {
    User getCurrentUser(HttpServletRequest request, HttpServletResponse response);
}
