package com.tencent.supersonic.auth.authentication.service;

import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.service.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class UserServiceCurrentUserProvider implements CurrentUserProvider {

    private final UserService userService;

    public UserServiceCurrentUserProvider(UserService userService) {
        this.userService = userService;
    }

    @Override
    public User getCurrentUser(HttpServletRequest request, HttpServletResponse response) {
        return userService.getCurrentUser(request, response);
    }
}
