package com.ardom.automotive_event_api.user;

import com.ardom.automotive_event_api.user.dto.request.UpdateProfileRequest;
import com.ardom.automotive_event_api.user.dto.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userMapper.toResponse(userService.getCurrentUser(authentication)));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                      Authentication authentication){
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(userMapper.toResponse(userService.updateProfile(user.getId(), request)));
    }
}
