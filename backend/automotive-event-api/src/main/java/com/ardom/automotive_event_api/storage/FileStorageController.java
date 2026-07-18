package com.ardom.automotive_event_api.storage;

import com.ardom.automotive_event_api.application.exception.InvalidPhotoKeyException;
import com.ardom.automotive_event_api.common.dto.response.ErrorResponse;
import com.ardom.automotive_event_api.storage.dto.request.PresignedUploadRequest;
import com.ardom.automotive_event_api.storage.dto.response.PresignedDownloadResponse;
import com.ardom.automotive_event_api.storage.dto.response.PresignedUploadResponse;
import com.ardom.automotive_event_api.user.User;
import com.ardom.automotive_event_api.user.exception.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
@Tag(name = "Storage", description = "Pre-signed S3 URL generation for client-side file upload and download")
@SecurityRequirement(name = "BearerAuth")
public class FileStorageController {

    private final FileStorageService fileStorageService;

    @Operation(
            summary = "Generate a pre-signed upload URL",
            description = """
                    Returns a pre-signed S3 PUT URL the client can use to upload a photo directly to S3 —
                    the file never passes through this API server.
                    
                    **Upload flow:**
                    1. Call this endpoint to get `uploadUrl` and `key`
                    2. PUT the file directly to `uploadUrl` with the matching `Content-Type` header
                    3. Store the returned `key` and include it in `CarDto.photoKeys` when updating an application
                    
                    **Constraints:**
                    - Supported types: `image/jpeg`, `image/png`, `image/webp`
                    - File extension must match the declared `contentType`
                    - File size must not exceed the configured maximum
                    - The URL expires at `expiresAt` — generate a new one if it lapses
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pre-signed upload URL generated successfully",
                    content = @Content(schema = @Schema(implementation = PresignedUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = """
                    One of:
                    - Request body validation failed
                    - Unsupported content type
                    - File extension does not match the declared content type
                    - File size exceeds the configured maximum
                    """,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content)
    })
    @PostMapping("/upload-url")
    public ResponseEntity<PresignedUploadResponse> getPresignedUploadUrl(
            Authentication authentication,
            @Valid @RequestBody PresignedUploadRequest request
    ) {
        User user = (User) authentication.getPrincipal();
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }

        return ResponseEntity.ok(
                fileStorageService.generateUploadUrl(user.getId(), request)
        );
    }

    @Operation(
            summary = "Generate a pre-signed download URL",
            description = """
                    Returns a pre-signed S3 GET URL for downloading a previously uploaded photo.
                    The key must belong to the authenticated user — keys are validated by extracting
                    the user ID from the key path (`photos/<userId>/...`).
                    
                    The URL expires at `expiresAt`. Call this endpoint again to get a fresh URL.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pre-signed download URL generated successfully",
                    content = @Content(schema = @Schema(implementation = PresignedDownloadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Key does not belong to the authenticated user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content)
    })
    @GetMapping("/download-url")
    public ResponseEntity<PresignedDownloadResponse> getPresignedDownloadUrl(
            Authentication authentication,
            @Parameter(description = "S3 object key of the file to download. Format: `photos/<userId>/<uuid>.<ext>`",
                    example = "photos/42/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg")
            @RequestParam String key
    ) {
        User user = (User) authentication.getPrincipal();
        if (!isUserOwnPhoto(user.getId(), key)) {
            throw new InvalidPhotoKeyException("Invalid photo key");
        }

        return ResponseEntity.ok(fileStorageService.generateDownloadUrl(key));
    }

    @Operation(
            summary = "Delete an uploaded file",
            description = """
                    Permanently deletes a file from S3.
                    The key must belong to the authenticated user — keys are validated by extracting
                    the user ID from the key path (`photos/<userId>/...`).
                    
                    **Note:** This does not remove the key from any application that has already
                    referenced it. Remove the car from the application before deleting its photos.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "File deleted, no content returned"),
            @ApiResponse(responseCode = "400", description = "Key does not belong to the authenticated user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "S3 deletion failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteObject(
            Authentication authentication,
            @Parameter(description = "S3 object key of the file to delete. Format: `photos/<userId>/<uuid>.<ext>`",
                    example = "photos/42/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg")
            @RequestParam String key
    ) {
        User user = (User) authentication.getPrincipal();
        if (!isUserOwnPhoto(user.getId(), key)) {
            throw new InvalidPhotoKeyException("Invalid photo key");
        }

        fileStorageService.delete(key);

        return ResponseEntity.noContent().build();
    }

    private boolean isUserOwnPhoto(Long userId, String key) {
        Long extractedUserId = Long.valueOf(key.split("/")[1]);
        return userId.equals(extractedUserId);
    }
}
