package com.ardom.automotive_event_api.application;

import com.ardom.automotive_event_api.application.dto.request.RejectApplicationRequest;
import com.ardom.automotive_event_api.application.dto.response.AdminApplicationResponse;
import com.ardom.automotive_event_api.application.dto.response.ApplicationSummaryResponse;
import com.ardom.automotive_event_api.application.exception.ApplicationNotFoundException;
import com.ardom.automotive_event_api.application.exception.InvalidApplicationStatusTransitionException;
import com.ardom.automotive_event_api.security.JwtService;
import com.ardom.automotive_event_api.security.config.TestSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminApplicationController.class)
@Import(TestSecurityConfig.class)
@DisplayName("AdminApplicationController")
class AdminApplicationControllerTest {

    private static final String BASE_URL = "/admin/applications";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ApplicationService applicationService;

    private AdminApplicationResponse sampleAdminResponse(Long id, ApplicationStatus status) {
        return new AdminApplicationResponse(
                id, null, "Spring Show", status, BigDecimal.TEN, List.of(), null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    // -------------------------------------------------------------------------
    // Method getAllApplications()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/applications")
    class GetAllApplications {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("getAllApplications_shouldReturn200WithPage_whenCalledByAdmin")
        void getAllApplications_shouldReturn200WithPage_whenCalledByAdmin() throws Exception {
            // given
            ApplicationSummaryResponse summary =
                    new ApplicationSummaryResponse(1L, ApplicationStatus.PENDING, "Spring Show", List.of());
            when(applicationService.getAllApplications(any()))
                    .thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 10), 1));

            // when / then
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("getAllApplications_shouldReturn403_whenCallerIsNotAdmin")
        void getAllApplications_shouldReturn403_whenCallerIsNotAdmin() throws Exception {
            // when / then
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // Method getApplication()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/applications/{id}")
    class GetApplication {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("getApplication_shouldReturn200_whenApplicationExists")
        void getApplication_shouldReturn200_whenApplicationExists() throws Exception {
            // given
            when(applicationService.getApplicationForAdmin(anyLong()))
                    .thenReturn(sampleAdminResponse(5L, ApplicationStatus.PENDING));

            // when / then
            mockMvc.perform(get(BASE_URL + "/{id}", 5L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("getApplication_shouldReturn404_whenApplicationDoesNotExist")
        void getApplication_shouldReturn404_whenApplicationDoesNotExist() throws Exception {
            // given
            when(applicationService.getApplicationForAdmin(anyLong()))
                    .thenThrow(new ApplicationNotFoundException("Application with id 5 is not found"));

            // when / then
            mockMvc.perform(get(BASE_URL + "/{id}", 5L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Application with id 5 is not found"));
        }
    }

    // -------------------------------------------------------------------------
    // Method approveApplication()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/applications/{id}/approve")
    class ApproveApplication {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("approveApplication_shouldReturn200_whenApplicationIsPending")
        void approveApplication_shouldReturn200_whenApplicationIsPending() throws Exception {
            // given
            when(applicationService.approveApplication(anyLong()))
                    .thenReturn(sampleAdminResponse(5L, ApplicationStatus.APPROVED_WAITING_PAYMENT));

            // when / then
            mockMvc.perform(post(BASE_URL + "/{id}/approve", 5L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED_WAITING_PAYMENT"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("approveApplication_shouldReturn404_whenApplicationDoesNotExist")
        void approveApplication_shouldReturn404_whenApplicationDoesNotExist() throws Exception {
            // given
            when(applicationService.approveApplication(anyLong()))
                    .thenThrow(new ApplicationNotFoundException("Application with id 5 is not found"));

            // when / then
            mockMvc.perform(post(BASE_URL + "/{id}/approve", 5L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Application with id 5 is not found"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("approveApplication_shouldReturn400_whenApplicationIsNotPending")
        void approveApplication_shouldReturn400_whenApplicationIsNotPending() throws Exception {
            // given
            when(applicationService.approveApplication(anyLong()))
                    .thenThrow(new InvalidApplicationStatusTransitionException("Application can't be approved"));

            // when / then
            mockMvc.perform(post(BASE_URL + "/{id}/approve", 5L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Application can't be approved"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("approveApplication_shouldReturn403_whenCallerIsNotAdmin")
        void approveApplication_shouldReturn403_whenCallerIsNotAdmin() throws Exception {
            // when / then
            mockMvc.perform(post(BASE_URL + "/{id}/approve", 5L))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // Method rejectApplication()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/applications/{id}/reject")
    class RejectApplication {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("rejectApplication_shouldReturn200_whenRequestIsValid")
        void rejectApplication_shouldReturn200_whenRequestIsValid() throws Exception {
            // given
            RejectApplicationRequest request = new RejectApplicationRequest("Photos don't meet requirements");
            when(applicationService.rejectApplication(anyLong(), any(RejectApplicationRequest.class)))
                    .thenReturn(sampleAdminResponse(5L, ApplicationStatus.REJECTED));

            // when / then
            mockMvc.perform(post(BASE_URL + "/{id}/reject", 5L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("rejectApplication_shouldReturn400_whenRejectionReasonExceedsMaxLength")
        void rejectApplication_shouldReturn400_whenRejectionReasonExceedsMaxLength() throws Exception {
            // given
            String tooLong = "a".repeat(201);
            RejectApplicationRequest request = new RejectApplicationRequest(tooLong);

            // when / then
            mockMvc.perform(post(BASE_URL + "/{id}/reject", 5L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("rejectApplication_shouldReturn404_whenApplicationDoesNotExist")
        void rejectApplication_shouldReturn404_whenApplicationDoesNotExist() throws Exception {
            // given
            RejectApplicationRequest request = new RejectApplicationRequest("Bad photos");
            when(applicationService.rejectApplication(anyLong(), any(RejectApplicationRequest.class)))
                    .thenThrow(new ApplicationNotFoundException("Application with id 5 is not found"));

            // when / then
            mockMvc.perform(post(BASE_URL + "/{id}/reject", 5L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Application with id 5 is not found"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("rejectApplication_shouldReturn400_whenApplicationIsNotPending")
        void rejectApplication_shouldReturn400_whenApplicationIsNotPending() throws Exception {
            // given
            RejectApplicationRequest request = new RejectApplicationRequest("Bad photos");
            when(applicationService.rejectApplication(anyLong(), any(RejectApplicationRequest.class)))
                    .thenThrow(new InvalidApplicationStatusTransitionException("Application can't be rejected"));

            // when / then
            mockMvc.perform(post(BASE_URL + "/{id}/reject", 5L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Application can't be rejected"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("rejectApplication_shouldReturn403_whenCallerIsNotAdmin")
        void rejectApplication_shouldReturn403_whenCallerIsNotAdmin() throws Exception {
            // given
            RejectApplicationRequest request = new RejectApplicationRequest("Bad photos");

            // when / then
            mockMvc.perform(post(BASE_URL + "/{id}/reject", 5L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}
