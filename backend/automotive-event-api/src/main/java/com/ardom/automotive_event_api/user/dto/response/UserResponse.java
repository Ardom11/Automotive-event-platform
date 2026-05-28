package com.ardom.automotive_event_api.user.dto.response;

import com.ardom.automotive_event_api.user.Role;

public record UserResponse(
        Long id,
        String name,
        String surname,
        String email,
        Role role
) {}