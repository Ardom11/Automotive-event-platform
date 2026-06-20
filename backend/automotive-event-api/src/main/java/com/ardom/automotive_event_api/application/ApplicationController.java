package com.ardom.automotive_event_api.application;

import com.ardom.automotive_event_api.application.dto.request.CreateApplicationRequest;
import com.ardom.automotive_event_api.application.dto.request.UpdateApplicationRequest;
import com.ardom.automotive_event_api.application.dto.response.ApplicationResponse;
import com.ardom.automotive_event_api.application.dto.response.ApplicationSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(Authentication authentication,
                                                                 @Valid @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.ok(applicationService.createApplication(authentication, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApplicationResponse> updateApplication(Authentication authentication,
                                                                 @PathVariable Long id,
                                                                 @Valid @RequestBody UpdateApplicationRequest request) {
        return ResponseEntity.ok(applicationService.updateApplication(authentication, id, request));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApplicationResponse> submitApplication(Authentication authentication,
                                                                 @PathVariable Long id) {
        return ResponseEntity.ok(applicationService.submitApplication(authentication, id));
    }

    @GetMapping
    public ResponseEntity<Page<ApplicationSummaryResponse>> getAllUserApplications(Authentication authentication,
                                                                                   Pageable pageable) {
        return ResponseEntity.ok(applicationService.getUserApplications(authentication, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getUserApplication(Authentication authentication,
                                                                  @PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getApplication(authentication, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(Authentication authentication,
                                                  @PathVariable Long id) {
        applicationService.deleteApplication(authentication, id);
        return ResponseEntity.noContent().build();
    }
}
