package com.ardom.automotive_event_api.application;

import com.ardom.automotive_event_api.application.dto.request.CreateApplicationRequest;
import com.ardom.automotive_event_api.application.dto.request.UpdateApplicationRequest;
import com.ardom.automotive_event_api.application.dto.response.ApplicationResponse;
import com.ardom.automotive_event_api.application.dto.response.ApplicationSummaryResponse;
import com.ardom.automotive_event_api.common.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/applications")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "Manage event applications for the authenticated user")
@SecurityRequirement(name = "BearerAuth")
public class ApplicationController {

    private final ApplicationService applicationService;

    @Operation(
            summary = "Create a new application",
            description = """
                    Creates a blank DRAFT application for a published event.
                    
                    **Business rules:**
                    - The event must exist and have status `PUBLISHED`
                    - Today's date must be before the event's application deadline
                    - The authenticated user must not already have an application for the same event
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application created successfully",
                    content = @Content(schema = @Schema(implementation = ApplicationResponse.class))),
            @ApiResponse(responseCode = "400", description = """
                    One of:
                    - Request body validation failed
                    - Application deadline has already passed (`ApplicationDeadlineException`)
                    - Event is not in PUBLISHED status (treated as not found)
                    """,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Event not found or not published",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "User already has an application for this event",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(Authentication authentication,
                                                                 @Valid @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.ok(applicationService.createApplication(authentication, request));
    }

    @Operation(
            summary = "Add cars to an application",
            description = """
                    Appends new cars (with photos) to an existing application.
                    
                    **Business rules:**
                    - Application must belong to the authenticated user
                    - Application must be in `DRAFT`, `PENDING`, or `REJECTED` status
                    - Total car count across all updates must not exceed the configured maximum (default: 5)
                    - All photo keys must belong to the authenticated user (key format: `<prefix>/<userId>/...`)
                    - Cars cannot be removed via this endpoint — only added
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cars added and full application returned",
                    content = @Content(schema = @Schema(implementation = ApplicationResponse.class))),
            @ApiResponse(responseCode = "400", description = """
                    One of:
                    - Request body validation failed
                    - Application is not in an editable status (`DRAFT`, `PENDING`, `REJECTED`)
                    - Adding the cars would exceed the maximum allowed per application
                    - One or more photo keys do not belong to the authenticated user
                    """,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Application not found or belongs to a different user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ApplicationResponse> updateApplication(Authentication authentication,
                                                                 @PathVariable Long id,
                                                                 @Valid @RequestBody UpdateApplicationRequest request) {
        return ResponseEntity.ok(applicationService.updateApplication(authentication, id, request));
    }

    @Operation(
            summary = "Submit an application",
            description = """
                    Transitions the application from `DRAFT` or `REJECTED` → `PENDING`.
                    Triggers a confirmation email to the user.
                    
                    **Business rules:**
                    - Application must belong to the authenticated user
                    - Application must be in `DRAFT` or `REJECTED` status
                    - Application must have at least one car attached
                    - Event date must still be more than 2 weeks away; if not, the application
                      is automatically moved to `EXPIRED` and a 400 is returned
                    - Fee is calculated as: `event.applicationFee × numberOfCars`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application submitted, status is now PENDING",
                    content = @Content(schema = @Schema(implementation = ApplicationResponse.class))),
            @ApiResponse(responseCode = "400", description = """
                    One of:
                    - Application is not in `DRAFT` or `REJECTED` status
                    - Application has no cars attached
                    - Submission window has closed (less than 2 weeks before the event) — application is set to `EXPIRED`
                    """,
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Application not found or belongs to a different user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApplicationResponse> submitApplication(Authentication authentication,
                                                                 @PathVariable Long id) {
        return ResponseEntity.ok(applicationService.submitApplication(authentication, id));
    }

    @Operation(
            summary = "List all applications for the current user",
            description = "Returns a paginated list of application summaries for the authenticated user. " +
                    "Supports standard Pageable query params: `page`, `size`, `sort`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list returned (may be empty)",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<Page<ApplicationSummaryResponse>> getAllUserApplications(Authentication authentication,
                                                                                   Pageable pageable) {
        return ResponseEntity.ok(applicationService.getUserApplications(authentication, pageable));
    }

    @Operation(
            summary = "Get a single application",
            description = "Returns full application details including all cars and their photos. " +
                    "Only the owner can access their own application."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application found",
                    content = @Content(schema = @Schema(implementation = ApplicationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Application not found or belongs to a different user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getUserApplication(Authentication authentication,
                                                                  @PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getApplication(authentication, id));
    }

    @Operation(
            summary = "Delete an application",
            description = """
                    Permanently deletes an application regardless of its current status.
                    Only the owner can delete their own application.
                    
                    **Note:** Unlike update/submit, there is no status guard here —
                    deletion is permitted at any status. Ensure this is intentional.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted successfully, no content returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Application not found or belongs to a different user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(Authentication authentication,
                                                  @PathVariable Long id) {
        applicationService.deleteApplication(authentication, id);
        return ResponseEntity.noContent().build();
    }
}
