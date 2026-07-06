package com.ardom.automotive_event_api.application;

import com.ardom.automotive_event_api.application.dto.CarDto;
import com.ardom.automotive_event_api.application.dto.request.CreateApplicationRequest;
import com.ardom.automotive_event_api.application.dto.request.UpdateApplicationRequest;
import com.ardom.automotive_event_api.application.dto.response.ApplicationResponse;
import com.ardom.automotive_event_api.application.dto.response.ApplicationSummaryResponse;
import com.ardom.automotive_event_api.application.exception.*;
import com.ardom.automotive_event_api.event.exception.EventNotFoundException;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
@Import(TestSecurityConfig.class)
@DisplayName("ApplicationController")
class ApplicationControllerTest {

    private static final String BASE_URL = "/applications";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;


    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ApplicationService applicationService;

    private ApplicationResponse sampleApplicationResponse(Long id) {
        return new ApplicationResponse(
                id, "Spring Show", ApplicationStatus.DRAFT, BigDecimal.ZERO, List.of(), null);
    }

    // -------------------------------------------------------------------------
    // Method createApplication()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /applications")
    class CreateApplication {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("createApplication_shouldReturn200_whenRequestIsValid")
        void createApplication_shouldReturn200_whenRequestIsValid() throws Exception {
            // given
            CreateApplicationRequest request = new CreateApplicationRequest(1L);
            when(applicationService.createApplication(any(Authentication.class), any(CreateApplicationRequest.class)))
                    .thenReturn(sampleApplicationResponse(100L));

            // when / then
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100))
                    .andExpect(jsonPath("$.status").value("DRAFT"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("createApplication_shouldReturn400_whenEventIdIsMissing")
        void createApplication_shouldReturn400_whenEventIdIsMissing() throws Exception {
            // given
            String invalidBody = "{}";

            // when / then
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("createApplication_shouldReturn404_whenEventDoesNotExist")
        void createApplication_shouldReturn404_whenEventDoesNotExist() throws Exception {
            // given
            CreateApplicationRequest request = new CreateApplicationRequest(999L);
            when(applicationService.createApplication(any(Authentication.class), any(CreateApplicationRequest.class)))
                    .thenThrow(new EventNotFoundException("Event with id 999 is not found"));

            // when / then
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Event with id 999 is not found"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("createApplication_shouldReturn409_whenUserAlreadyHasApplicationForEvent")
        void createApplication_shouldReturn409_whenUserAlreadyHasApplicationForEvent() throws Exception {
            // given
            CreateApplicationRequest request = new CreateApplicationRequest(1L);
            when(applicationService.createApplication(any(Authentication.class), any(CreateApplicationRequest.class)))
                    .thenThrow(new UserAlreadyHasApplicationException("An application for event 1 already exists"));

            // when / then
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("An application for event 1 already exists"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("createApplication_shouldReturn400_whenPastApplicationDeadline")
        void createApplication_shouldReturn400_whenPastApplicationDeadline() throws Exception {
            // given
            CreateApplicationRequest request = new CreateApplicationRequest(1L);
            when(applicationService.createApplication(any(Authentication.class), any(CreateApplicationRequest.class)))
                    .thenThrow(new ApplicationDeadlineException("Application cannot be created less than 2 weeks before event"));

            // when / then
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Application cannot be created less than 2 weeks before event"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("createApplication_shouldReturn403_whenCallerLacksUserRole")
        void createApplication_shouldReturn403_whenCallerLacksUserRole() throws Exception {
            // given
            CreateApplicationRequest request = new CreateApplicationRequest(1L);

            // when / then
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // Method updateApplication()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("PATCH /applications/{id}")
    class UpdateApplication {

        private CarDto givenCarDto() {
            return new CarDto("BMW", "M3", (short) 2020, "story", List.of("photos/1/uuid.jpg"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("updateApplication_shouldReturn200_whenRequestIsValid")
        void updateApplication_shouldReturn200_whenRequestIsValid() throws Exception {
            // given
            UpdateApplicationRequest request = new UpdateApplicationRequest(List.of(givenCarDto()));
            when(applicationService.updateApplication(any(Authentication.class), anyLong(), any(UpdateApplicationRequest.class)))
                    .thenReturn(sampleApplicationResponse(5L));

            // when / then
            mockMvc.perform(patch(BASE_URL + "/{id}", 5L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("updateApplication_shouldReturn400_whenCarsListIsEmpty")
        void updateApplication_shouldReturn400_whenCarsListIsEmpty() throws Exception {
            // given
            UpdateApplicationRequest request = new UpdateApplicationRequest(List.of());

            // when / then
            mockMvc.perform(patch(BASE_URL + "/{id}", 5L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("updateApplication_shouldReturn404_whenApplicationDoesNotExist")
        void updateApplication_shouldReturn404_whenApplicationDoesNotExist() throws Exception {
            // given
            UpdateApplicationRequest request = new UpdateApplicationRequest(List.of(givenCarDto()));
            when(applicationService.updateApplication(any(Authentication.class), anyLong(), any(UpdateApplicationRequest.class)))
                    .thenThrow(new ApplicationNotFoundException("Application with id 5 is not found"));

            // when / then
            mockMvc.perform(patch(BASE_URL + "/{id}", 5L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("updateApplication_shouldReturn400_whenApplicationNotEditable")
        void updateApplication_shouldReturn400_whenApplicationNotEditable() throws Exception {
            // given
            UpdateApplicationRequest request = new UpdateApplicationRequest(List.of(givenCarDto()));
            when(applicationService.updateApplication(any(Authentication.class), anyLong(), any(UpdateApplicationRequest.class)))
                    .thenThrow(new ApplicationNotEditableException("Application with status COMPLETED cannot be edited"));

            // when / then
            mockMvc.perform(patch(BASE_URL + "/{id}", 5L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Application with status COMPLETED cannot be edited"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("updateApplication_shouldReturn400_whenTooManyCars")
        void updateApplication_shouldReturn400_whenTooManyCars() throws Exception {
            // given
            UpdateApplicationRequest request = new UpdateApplicationRequest(List.of(givenCarDto()));
            when(applicationService.updateApplication(any(Authentication.class), anyLong(), any(UpdateApplicationRequest.class)))
                    .thenThrow(new TooManyCarsException("Application already has 5 car(s)"));

            // when / then
            mockMvc.perform(patch(BASE_URL + "/{id}", 5L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Application already has 5 car(s)"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("updateApplication_shouldReturn400_whenPhotoKeyIsInvalid")
        void updateApplication_shouldReturn400_whenPhotoKeyIsInvalid() throws Exception {
            // given
            UpdateApplicationRequest request = new UpdateApplicationRequest(List.of(givenCarDto()));
            when(applicationService.updateApplication(any(Authentication.class), anyLong(), any(UpdateApplicationRequest.class)))
                    .thenThrow(new InvalidPhotoKeyException("Invalid photo key: applications/1/uuid.jpg"));

            // when / then
            mockMvc.perform(patch(BASE_URL + "/{id}", 5L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid photo key: applications/1/uuid.jpg"));
        }
    }

    // -------------------------------------------------------------------------
    // Method submitApplication()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /applications/{id}/submit")
    class SubmitApplication {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("submitApplication_shouldReturn200_whenApplicationIsSubmittable")
        void submitApplication_shouldReturn200_whenApplicationIsSubmittable() throws Exception {
            // given
            when(applicationService.submitApplication(any(Authentication.class), anyLong()))
                    .thenReturn(sampleApplicationResponse(5L));

            // when / then
            mockMvc.perform(post(BASE_URL + "/{id}/submit", 5L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("submitApplication_shouldReturn404_whenApplicationDoesNotExist")
        void submitApplication_shouldReturn404_whenApplicationDoesNotExist() throws Exception {
            // given
            when(applicationService.submitApplication(any(Authentication.class), anyLong()))
                    .thenThrow(new ApplicationNotFoundException("Application with id 5 is not found"));

            // when / then
            mockMvc.perform(post(BASE_URL + "/{id}/submit", 5L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Application with id 5 is not found"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("submitApplication_shouldReturn400_whenApplicationHasNoCars")
        void submitApplication_shouldReturn400_whenApplicationHasNoCars() throws Exception {
            // given
            when(applicationService.submitApplication(any(Authentication.class), anyLong()))
                    .thenThrow(new ApplicationNotSubmittableException("Cannot submit an application with no cars"));

            // when / then
            mockMvc.perform(post(BASE_URL + "/{id}/submit", 5L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cannot submit an application with no cars"));
        }
    }

    // -------------------------------------------------------------------------
    // Method getAllUserApplications()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /applications")
    class GetAllUserApplications {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("getAllUserApplications_shouldReturn200WithPage_whenCalled")
        void getAllUserApplications_shouldReturn200WithPage_whenCalled() throws Exception {
            // given
            ApplicationSummaryResponse summary =
                    new ApplicationSummaryResponse(1L, ApplicationStatus.PENDING, "Spring Show", List.of());
            when(applicationService.getUserApplications(any(Authentication.class), any()))
                    .thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 10), 1));

            // when / then
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    // -------------------------------------------------------------------------
    // Method getUserApplication()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /applications/{id}")
    class GetUserApplication {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("getUserApplication_shouldReturn200_whenApplicationBelongsToCaller")
        void getUserApplication_shouldReturn200_whenApplicationBelongsToCaller() throws Exception {
            // given
            when(applicationService.getApplication(any(Authentication.class), anyLong()))
                    .thenReturn(sampleApplicationResponse(5L));

            // when / then
            mockMvc.perform(get(BASE_URL + "/{id}", 5L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("getUserApplication_shouldReturn404_whenApplicationDoesNotBelongToCaller")
        void getUserApplication_shouldReturn404_whenApplicationDoesNotBelongToCaller() throws Exception {
            // given
            when(applicationService.getApplication(any(Authentication.class), anyLong()))
                    .thenThrow(new ApplicationNotFoundException("Application with id 5 is not found"));

            // when / then
            mockMvc.perform(get(BASE_URL + "/{id}", 5L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Application with id 5 is not found"));
        }
    }

    // -------------------------------------------------------------------------
    // Method deleteApplication()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("DELETE /applications/{id}")
    class DeleteApplication {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("deleteApplication_shouldReturn204_whenApplicationBelongsToCaller")
        void deleteApplication_shouldReturn204_whenApplicationBelongsToCaller() throws Exception {
            // when / then
            mockMvc.perform(delete(BASE_URL + "/{id}", 5L))
                    .andExpect(status().isNoContent());

            verify(applicationService).deleteApplication(any(Authentication.class), org.mockito.ArgumentMatchers.eq(5L));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("deleteApplication_shouldReturn404_whenApplicationDoesNotBelongToCaller")
        void deleteApplication_shouldReturn404_whenApplicationDoesNotBelongToCaller() throws Exception {
            // given
            org.mockito.Mockito.doThrow(new ApplicationNotFoundException("Application with id 5 is not found"))
                    .when(applicationService).deleteApplication(any(Authentication.class), anyLong());

            // when / then
            mockMvc.perform(delete(BASE_URL + "/{id}", 5L))
                    .andExpect(status().isNotFound());
        }
    }
}
