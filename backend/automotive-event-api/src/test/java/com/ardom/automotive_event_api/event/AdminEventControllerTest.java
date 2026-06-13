package com.ardom.automotive_event_api.event;

import com.ardom.automotive_event_api.event.dto.LocationDto;
import com.ardom.automotive_event_api.event.dto.request.CreateEventRequest;
import com.ardom.automotive_event_api.event.dto.request.UpdateEventRequest;
import com.ardom.automotive_event_api.event.dto.request.UpdateEventStatusRequest;
import com.ardom.automotive_event_api.event.dto.response.AdminEventSummaryResponse;
import com.ardom.automotive_event_api.event.dto.response.EventResponse;
import com.ardom.automotive_event_api.event.exception.EventNotFoundException;
import com.ardom.automotive_event_api.event.exception.InvalidStatusTransitionException;
import com.ardom.automotive_event_api.security.JwtService;
import com.ardom.automotive_event_api.security.config.TestSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminEventController.class)
@Import(TestSecurityConfig.class)
@DisplayName("AdminEventController")
class AdminEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private Authentication adminAuth;
    private Authentication userAuth;

    private LocationDto locationDto;
    private EventResponse eventResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        adminAuth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        userAuth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        locationDto = new LocationDto(
                "Circuit Park", "Main Street 1", "Amsterdam", "Netherlands",
                new BigDecimal("52.365400"), new BigDecimal("4.904100")
        );

        eventResponse = new EventResponse(
                1L, "Festival1", "Very interesting automotive event",
                locationDto,
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 8, 3, 22, 0),
                LocalDate.of(2026, 7, 18),
                LocalDate.of(2026, 7, 25),
                3000, new BigDecimal("60.00"), new BigDecimal("110.00"),
                EventStatus.DRAFT,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }

    // -------------------------------------------------------------------------
    // POST /admin/events
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /admin/events")
    class CreateEvent {

        private CreateEventRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new CreateEventRequest(
                    "Festival1", "Very interesting automotive event",
                    "Circuit Park", "Main Street 1", "Amsterdam", "Netherlands",
                    new BigDecimal("52.365400"), new BigDecimal("4.904100"),
                    LocalDateTime.of(2026, 8, 1, 10, 0),
                    LocalDateTime.of(2026, 8, 3, 22, 0),
                    3000, new BigDecimal("60.00"), new BigDecimal("110.00")
            );
        }

        @Test
        @DisplayName("Should return 201 with created event when request is valid")
        void createEvent_shouldReturn201WithCreatedEvent_whenRequestIsValid() throws Exception {
            when(eventService.createEvent(any(CreateEventRequest.class))).thenReturn(eventResponse);

            mockMvc.perform(post("/admin/events")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Festival1"))
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.location.city").value("Amsterdam"));
        }

        @Test
        @DisplayName("Should return 400 when name is blank")
        void createEvent_shouldReturn400_whenNameIsBlank() throws Exception {
            CreateEventRequest invalidRequest = new CreateEventRequest(
                    "", "Very interesting automotive event",
                    "Circuit Park", "Main Street 1", "Amsterdam", "Netherlands",
                    null, null,
                    LocalDateTime.of(2026, 8, 1, 10, 0),
                    LocalDateTime.of(2026, 8, 3, 22, 0),
                    3000, new BigDecimal("60.00"), new BigDecimal("110.00")
            );

            mockMvc.perform(post("/admin/events")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when dateStart is in the past")
        void createEvent_shouldReturn400_whenDateStartIsInThePast() throws Exception {
            CreateEventRequest invalidRequest = new CreateEventRequest(
                    "Festival1", "Very interesting automotive event",
                    "Circuit Park", "Main Street 1", "Amsterdam", "Netherlands",
                    null, null,
                    LocalDateTime.of(2020, 1, 1, 10, 0),
                    LocalDateTime.of(2026, 8, 3, 22, 0),
                    3000, new BigDecimal("60.00"), new BigDecimal("110.00")
            );

            mockMvc.perform(post("/admin/events")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when ticketPrice is negative")
        void createEvent_shouldReturn400_whenTicketPriceIsNegative() throws Exception {
            CreateEventRequest invalidRequest = new CreateEventRequest(
                    "Festival1", "Very interesting automotive event",
                    "Circuit Park", "Main Street 1", "Amsterdam", "Netherlands",
                    null, null,
                    LocalDateTime.of(2026, 8, 1, 10, 0),
                    LocalDateTime.of(2026, 8, 3, 22, 0),
                    3000, new BigDecimal("-10.00"), new BigDecimal("110.00")
            );

            mockMvc.perform(post("/admin/events")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when user is not admin")
        void createEvent_shouldReturn403_whenUserIsNotAdmin() throws Exception {
            mockMvc.perform(post("/admin/events")
                            .with(authentication(userAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 when request is unauthenticated")
        void createEvent_shouldReturn401_whenUnauthenticated() throws Exception {
            mockMvc.perform(post("/admin/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // GET /admin/events
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /admin/events")
    class GetEvents {

        @Test
        @DisplayName("Should return 200 with page of events when called")
        void getEvents_shouldReturn200WithPageOfEvents_whenCalled() throws Exception {
            AdminEventSummaryResponse summary = new AdminEventSummaryResponse(
                    1L, "Festival1", "Amsterdam",
                    LocalDateTime.of(2026, 8, 1, 10, 0),
                    LocalDateTime.of(2026, 8, 3, 22, 0),
                    EventStatus.DRAFT
            );
            Page<AdminEventSummaryResponse> page = new PageImpl<>(List.of(summary));
            when(eventService.getAllEvents(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/admin/events")
                            .with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1L))
                    .andExpect(jsonPath("$.content[0].name").value("Festival1"))
                    .andExpect(jsonPath("$.content[0].city").value("Amsterdam"))
                    .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
        }

        @Test
        @DisplayName("Should return 200 with empty page when no events exist")
        void getEvents_shouldReturn200WithEmptyPage_whenNoEventsExist() throws Exception {
            when(eventService.getAllEvents(any(Pageable.class))).thenReturn(Page.empty());

            mockMvc.perform(get("/admin/events")
                            .with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("Should return 403 when user is not admin")
        void getEvents_shouldReturn403_whenUserIsNotAdmin() throws Exception {
            mockMvc.perform(get("/admin/events")
                            .with(authentication(userAuth)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 when request is unauthenticated")
        void getEvents_shouldReturn401_whenUnauthenticated() throws Exception {
            mockMvc.perform(get("/admin/events"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // GET /admin/events/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /admin/events/{id}")
    class GetEvent {

        @Test
        @DisplayName("Should return 200 with event when event exists")
        void getEvent_shouldReturn200WithEvent_whenEventExists() throws Exception {
            when(eventService.getFullEvent(1L)).thenReturn(eventResponse);

            mockMvc.perform(get("/admin/events/1")
                            .with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Festival1"))
                    .andExpect(jsonPath("$.description").value("Very interesting automotive event"));
        }

        @Test
        @DisplayName("Should return 404 when event does not exist")
        void getEvent_shouldReturn404_whenEventDoesNotExist() throws Exception {
            when(eventService.getFullEvent(99L)).thenThrow(new EventNotFoundException("Event with id 99 is not found"));

            mockMvc.perform(get("/admin/events/99")
                            .with(authentication(adminAuth)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not admin")
        void getEvent_shouldReturn403_whenUserIsNotAdmin() throws Exception {
            mockMvc.perform(get("/admin/events/1")
                            .with(authentication(userAuth)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 when request is unauthenticated")
        void getEvent_shouldReturn401_whenUnauthenticated() throws Exception {
            mockMvc.perform(get("/admin/events/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /admin/events/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /admin/events/{id}")
    class UpdateEvent {

        @Test
        @DisplayName("Should return 200 with updated event when request is valid")
        void updateEvent_shouldReturn200WithUpdatedEvent_whenRequestIsValid() throws Exception {
            UpdateEventRequest request = new UpdateEventRequest(
                    "Updated Festival", null, null, null, null, null,
                    null, null, null, null, null, null, null
            );
            EventResponse updatedResponse = new EventResponse(
                    1L, "Updated Festival", "Very interesting automotive event",
                    locationDto,
                    LocalDateTime.of(2026, 8, 1, 10, 0),
                    LocalDateTime.of(2026, 8, 3, 22, 0),
                    LocalDate.of(2026, 7, 18),
                    LocalDate.of(2026, 7, 25),
                    3000, new BigDecimal("60.00"), new BigDecimal("110.00"),
                    EventStatus.DRAFT,
                    LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2026, 1, 1, 0, 0)
            );
            when(eventService.updateEvent(eq(1L), any(UpdateEventRequest.class))).thenReturn(updatedResponse);

            mockMvc.perform(patch("/admin/events/1")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Festival"));
        }

        @Test
        @DisplayName("Should return 400 when name exceeds max length")
        void updateEvent_shouldReturn400_whenNameExceedsMaxLength() throws Exception {
            UpdateEventRequest request = new UpdateEventRequest(
                    "A".repeat(101), null, null, null, null, null,
                    null, null, null, null, null, null, null
            );

            mockMvc.perform(patch("/admin/events/1")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when event does not exist")
        void updateEvent_shouldReturn404_whenEventDoesNotExist() throws Exception {
            UpdateEventRequest request = new UpdateEventRequest(
                    "Updated Festival", null, null, null, null, null,
                    null, null, null, null, null, null, null
            );
            when(eventService.updateEvent(eq(99L), any(UpdateEventRequest.class)))
                    .thenThrow(new EventNotFoundException("Event with id 99 is not found"));

            mockMvc.perform(patch("/admin/events/99")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not admin")
        void updateEvent_shouldReturn403_whenUserIsNotAdmin() throws Exception {
            UpdateEventRequest request = new UpdateEventRequest(
                    "Updated Festival", null, null, null, null, null,
                    null, null, null, null, null, null, null
            );

            mockMvc.perform(patch("/admin/events/1")
                            .with(authentication(userAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 when request is unauthenticated")
        void updateEvent_shouldReturn401_whenUnauthenticated() throws Exception {
            UpdateEventRequest request = new UpdateEventRequest(
                    "Updated Festival", null, null, null, null, null,
                    null, null, null, null, null, null, null
            );

            mockMvc.perform(patch("/admin/events/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /admin/events/{id}/status
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /admin/events/{id}/status")
    class UpdateEventStatus {

        @Test
        @DisplayName("Should return 200 with updated event when transition is valid")
        void updateEventStatus_shouldReturn200_whenTransitionIsValid() throws Exception {
            UpdateEventStatusRequest request = new UpdateEventStatusRequest(EventStatus.PUBLISHED);
            EventResponse publishedResponse = new EventResponse(
                    1L, "Festival1", "Very interesting automotive event",
                    locationDto,
                    LocalDateTime.of(2026, 8, 1, 10, 0),
                    LocalDateTime.of(2026, 8, 3, 22, 0),
                    LocalDate.of(2026, 7, 18),
                    LocalDate.of(2026, 7, 25),
                    3000, new BigDecimal("60.00"), new BigDecimal("110.00"),
                    EventStatus.PUBLISHED,
                    LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2026, 1, 1, 0, 0)
            );
            when(eventService.updateEventStatus(1L, EventStatus.PUBLISHED)).thenReturn(publishedResponse);

            mockMvc.perform(patch("/admin/events/1/status")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PUBLISHED"));
        }

        @Test
        @DisplayName("Should return 409 when status transition is invalid")
        void updateEventStatus_shouldReturn409_whenTransitionIsInvalid() throws Exception {
            UpdateEventStatusRequest request = new UpdateEventStatusRequest(EventStatus.ARCHIVED);
            when(eventService.updateEventStatus(1L, EventStatus.ARCHIVED))
                    .thenThrow(new InvalidStatusTransitionException("Cannot change status from DRAFT to ARCHIVED"));

            mockMvc.perform(patch("/admin/events/1/status")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 400 when status is null")
        void updateEventStatus_shouldReturn400_whenStatusIsNull() throws Exception {
            UpdateEventStatusRequest request = new UpdateEventStatusRequest(null);

            mockMvc.perform(patch("/admin/events/1/status")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when event does not exist")
        void updateEventStatus_shouldReturn404_whenEventDoesNotExist() throws Exception {
            UpdateEventStatusRequest request = new UpdateEventStatusRequest(EventStatus.PUBLISHED);
            when(eventService.updateEventStatus(99L, EventStatus.PUBLISHED))
                    .thenThrow(new EventNotFoundException("Event with id 99 is not found"));

            mockMvc.perform(patch("/admin/events/99/status")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not admin")
        void updateEventStatus_shouldReturn403_whenUserIsNotAdmin() throws Exception {
            UpdateEventStatusRequest request = new UpdateEventStatusRequest(EventStatus.PUBLISHED);

            mockMvc.perform(patch("/admin/events/1/status")
                            .with(authentication(userAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 when request is unauthenticated")
        void updateEventStatus_shouldReturn401_whenUnauthenticated() throws Exception {
            UpdateEventStatusRequest request = new UpdateEventStatusRequest(EventStatus.PUBLISHED);

            mockMvc.perform(patch("/admin/events/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /admin/events/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /admin/events/{id}")
    class DeleteEvent {

        @Test
        @DisplayName("Should return 204 when event is deleted successfully")
        void deleteEvent_shouldReturn204_whenEventIsDeletedSuccessfully() throws Exception {
            doNothing().when(eventService).deleteEvent(1L);

            mockMvc.perform(delete("/admin/events/1")
                            .with(authentication(adminAuth)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 404 when event does not exist")
        void deleteEvent_shouldReturn404_whenEventDoesNotExist() throws Exception {
            doThrow(new EventNotFoundException("Event with id 99 is not found"))
                    .when(eventService).deleteEvent(99L);

            mockMvc.perform(delete("/admin/events/99")
                            .with(authentication(adminAuth)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user is not admin")
        void deleteEvent_shouldReturn403_whenUserIsNotAdmin() throws Exception {
            mockMvc.perform(delete("/admin/events/1")
                            .with(authentication(userAuth)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 when request is unauthenticated")
        void deleteEvent_shouldReturn401_whenUnauthenticated() throws Exception {
            mockMvc.perform(delete("/admin/events/1"))
                    .andExpect(status().isUnauthorized());
        }
    }
}