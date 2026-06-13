package com.ardom.automotive_event_api.event;

import com.ardom.automotive_event_api.event.dto.LocationDto;
import com.ardom.automotive_event_api.event.dto.response.EventSummaryResponse;
import com.ardom.automotive_event_api.event.exception.EventNotFoundException;
import com.ardom.automotive_event_api.security.JwtService;
import com.ardom.automotive_event_api.security.config.TestSecurityConfig;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import(TestSecurityConfig.class)
@DisplayName("EventController")
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private EventSummaryResponse eventSummaryResponse;

    @BeforeEach
    void setUp() {
        LocationDto locationDto = new LocationDto(
                "Circuit Park", "Main Street 1", "Amsterdam", "Netherlands",
                new BigDecimal("52.365400"), new BigDecimal("4.904100")
        );

        eventSummaryResponse = new EventSummaryResponse(
                "Festival1", "Very interesting automotive event",
                locationDto,
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 8, 3, 22, 0),
                new BigDecimal("60.00")
        );
    }

    // -------------------------------------------------------------------------
    // GET /events
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /events")
    class GetEvents {

        @Test
        @DisplayName("Should return 200 with page of published events when called")
        void getEvents_shouldReturn200WithPublishedEvents_whenCalled() throws Exception {
            Page<EventSummaryResponse> page = new PageImpl<>(List.of(eventSummaryResponse));
            when(eventService.getPublishedEvents(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/events"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Festival1"))
                    .andExpect(jsonPath("$.content[0].location.city").value("Amsterdam"))
                    .andExpect(jsonPath("$.content[0].ticketPrice").value(60.00));
        }

        @Test
        @DisplayName("Should return 200 with empty page when no published events exist")
        void getEvents_shouldReturn200WithEmptyPage_whenNoPublishedEventsExist() throws Exception {
            when(eventService.getPublishedEvents(any(Pageable.class))).thenReturn(Page.empty());

            mockMvc.perform(get("/events"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("Should return 200 without authentication")
        void getEvents_shouldReturn200_whenUnauthenticated() throws Exception {
            when(eventService.getPublishedEvents(any(Pageable.class))).thenReturn(Page.empty());

            mockMvc.perform(get("/events"))
                    .andExpect(status().isOk());
        }
    }

    // -------------------------------------------------------------------------
    // GET /events/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /events/{id}")
    class GetEvent {

        @Test
        @DisplayName("Should return 200 with event summary when event exists")
        void getEvent_shouldReturn200WithEventSummary_whenEventExists() throws Exception {
            when(eventService.getEventSummary(1L)).thenReturn(eventSummaryResponse);

            mockMvc.perform(get("/events/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Festival1"))
                    .andExpect(jsonPath("$.description").value("Very interesting automotive event"))
                    .andExpect(jsonPath("$.location.city").value("Amsterdam"))
                    .andExpect(jsonPath("$.ticketPrice").value(60.00));
        }

        @Test
        @DisplayName("Should return 404 when event does not exist")
        void getEvent_shouldReturn404_whenEventDoesNotExist() throws Exception {
            when(eventService.getEventSummary(99L))
                    .thenThrow(new EventNotFoundException("Event with id 99 is not found"));

            mockMvc.perform(get("/events/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 200 without authentication")
        void getEvent_shouldReturn200_whenUnauthenticated() throws Exception {
            when(eventService.getEventSummary(1L)).thenReturn(eventSummaryResponse);

            mockMvc.perform(get("/events/1"))
                    .andExpect(status().isOk());
        }
    }
}