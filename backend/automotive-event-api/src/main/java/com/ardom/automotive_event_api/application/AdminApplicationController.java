package com.ardom.automotive_event_api.application;

import com.ardom.automotive_event_api.application.dto.request.RejectApplicationRequest;
import com.ardom.automotive_event_api.application.dto.response.AdminApplicationResponse;
import com.ardom.automotive_event_api.application.dto.response.ApplicationSummaryResponse;
import com.ardom.automotive_event_api.common.dto.response.ErrorResponse;
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
@Tag(name = "Admin — Applications", description = "Admin operations for reviewing and managing all applications")
@SecurityRequirement(name = "BearerAuth")
public class AdminApplicationController {

    private final ApplicationService applicationService;

    @Operation(
            summary = "List all applications (all users)",
            description = "Returns a paginated list of application summaries across all users. " +
                    "Default page size is 10. Supports `page`, `size`, and `sort` query params."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list returned (may be empty)",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have ADMIN role",
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<Page<ApplicationSummaryResponse>> getAllApplications(
            @Parameter(hidden = true)
            @PageableDefault(size = 10)
            Pageable pageable
    ) {
        return ResponseEntity.ok(applicationService.getAllApplications(pageable));
    }

    @Operation(
            summary = "Get full application details",
            description = "Returns complete application details including user info, all cars, photos, " +
                    "fee, and audit timestamps. Accessible for any application regardless of owner."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application found",
                    content = @Content(schema = @Schema(implementation = AdminApplicationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have ADMIN role",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Application not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AdminApplicationResponse> getApplication(
            @Parameter(description = "ID of the application to retrieve", example = "42") @PathVariable Long id
    ) {
        return ResponseEntity.ok(applicationService.getApplicationForAdmin(id));
    }

    @Operation(
            summary = "Approve an application",
            description = """
                    Transitions the application from `PENDING` → `APPROVED_WAITING_PAYMENT`.
                    Triggers an approval email to the applicant.
                    
                    **Business rules:**
                    - Application must be in `PENDING` status; any other status returns 400
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application approved, status is now APPROVED_WAITING_PAYMENT",
                    content = @Content(schema = @Schema(implementation = AdminApplicationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Application is not in PENDING status",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have ADMIN role",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Application not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/approve")
    public ResponseEntity<AdminApplicationResponse> approveApplication(
            @Parameter(description = "ID of the application to approve", example = "42")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(applicationService.approveApplication(id));
    }

    @Operation(
            summary = "Reject an application",
            description = """
                    Transitions the application from `PENDING` → `REJECTED`.
                    Triggers a rejection email to the applicant with the optional reason.
                    The applicant may then edit and resubmit the application.
                    
                    **Business rules:**
                    - Application must be in `PENDING` status; any other status returns 400
                    - `rejectionReason` is optional but recommended — if blank, no reason is stored or emailed
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application rejected, status is now REJECTED",
                    content = @Content(schema = @Schema(implementation = AdminApplicationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Application is not in PENDING status, or request body is invalid",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have ADMIN role",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Application not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/reject")
    public ResponseEntity<AdminApplicationResponse> rejectApplication(
            @Parameter(description = "ID of the application to reject", example = "42")
            @PathVariable Long id,
            @Valid @RequestBody RejectApplicationRequest request
    ) {
        return ResponseEntity.ok(applicationService.rejectApplication(id, request));
    }
}
