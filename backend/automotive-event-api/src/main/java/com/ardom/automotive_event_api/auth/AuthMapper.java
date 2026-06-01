package com.ardom.automotive_event_api.auth;

import com.ardom.automotive_event_api.auth.dto.request.RegisterRequest;
import com.ardom.automotive_event_api.user.Role;
import com.ardom.automotive_event_api.user.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {
    public User toEntity(RegisterRequest registerRequest) {
        return User.builder()
                .email(registerRequest.email())
                .password(registerRequest.password())
                .role(Role.USER)
                .build();
    }
}
