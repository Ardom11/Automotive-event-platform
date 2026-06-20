package com.ardom.automotive_event_api.application;

import com.ardom.automotive_event_api.application.dto.request.RejectApplicationRequest;
import com.ardom.automotive_event_api.application.dto.response.AdminApplicationResponse;
import com.ardom.automotive_event_api.application.dto.response.ApplicationSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/applications")
@RequiredArgsConstructor
public class AdminApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public ResponseEntity<Page<ApplicationSummaryResponse>> getAllApplications(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(applicationService.getAllApplications(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminApplicationResponse> getApplication(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getApplicationForAdmin(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<AdminApplicationResponse> approveApplication(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.approveApplication(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<AdminApplicationResponse> rejectApplication(@PathVariable Long id,
                                                                      @Valid @RequestBody RejectApplicationRequest request) {
        return ResponseEntity.ok(applicationService.rejectApplication(id, request));
    }
}
