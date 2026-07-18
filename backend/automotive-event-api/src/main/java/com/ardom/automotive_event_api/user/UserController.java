package com.ardom.automotive_event_api.user;

import com.ardom.automotive_event_api.common.dto.response.ErrorResponse;
import com.ardom.automotive_event_api.user.dto.request.UpdateProfileRequest;
import com.ardom.automotive_event_api.user.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Profile management for the authenticated user")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @Operation(
            summary = "Get current user profile",
            description = "Returns the profile of the currently authenticated user, resolved directly from the JWT principal."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userMapper.toResponse(userService.getCurrentUser(authentication)));
    }

    @Operation(
            summary = "Update current user profile",
            description = """
                    Partially updates the authenticated user's first and/or last name.
                    
                    **Field behaviour:**
                    - Both fields are optional — only non-null, non-blank values are applied
                    - If both fields are null or blank the service performs no update and returns `null`,
                      which means the controller will return a `200` with a null body — consider
                      returning `400` in this case instead (see note below)
                    - Field length: 2–50 characters each
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request body validation failed (field too short or too long)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found (should not occur under normal conditions)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(userMapper.toResponse(userService.updateProfile(user.getId(), request)));
    }
}
