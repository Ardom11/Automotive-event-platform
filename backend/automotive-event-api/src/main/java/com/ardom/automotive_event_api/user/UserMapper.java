package com.ardom.automotive_event_api.user;

import com.ardom.automotive_event_api.user.dto.response.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(),
                user.getName(),
                user.getSurname(),
                user.getEmail(),
                user.getRole());
    }
}