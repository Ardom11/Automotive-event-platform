package com.ardom.automotive_event_api.storage;

import com.ardom.automotive_event_api.application.exception.InvalidPhotoKeyException;
import com.ardom.automotive_event_api.storage.dto.request.PresignedUploadRequest;
import com.ardom.automotive_event_api.storage.dto.response.PresignedDownloadResponse;
import com.ardom.automotive_event_api.storage.dto.response.PresignedUploadResponse;
import com.ardom.automotive_event_api.user.User;
import com.ardom.automotive_event_api.user.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class FileStorageController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload-url")
    public ResponseEntity<PresignedUploadResponse> getPresignedUploadUrl(
            Authentication authentication,
            @Valid @RequestBody PresignedUploadRequest request) {

        User user = (User) authentication.getPrincipal();
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }

        return ResponseEntity.ok(
                fileStorageService.generateUploadUrl(user.getId(), request.filename(), request.contentType())
        );
    }

    @GetMapping("/download-url")
    public ResponseEntity<PresignedDownloadResponse> getPresignedDownloadUrl(
            Authentication authentication, @RequestParam String key) {

        User user = (User) authentication.getPrincipal();
        if (!isUserOwnPhoto(user.getId(), key)) {
            throw new InvalidPhotoKeyException("Invalid photo key");
        }

        return ResponseEntity.ok(fileStorageService.generateDownloadUrl(key));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteObject(
            Authentication authentication, @RequestParam String key) {

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
