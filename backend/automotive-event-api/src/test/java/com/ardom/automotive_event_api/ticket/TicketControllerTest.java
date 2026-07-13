package com.ardom.automotive_event_api.ticket;

import com.ardom.automotive_event_api.security.JwtService;
import com.ardom.automotive_event_api.security.config.TestSecurityConfig;
import com.ardom.automotive_event_api.ticket.dto.response.TicketSummaryResponse;
import com.ardom.automotive_event_api.ticket.exception.TicketNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
@Import(TestSecurityConfig.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private TicketService ticketService;

    private TicketSummaryResponse ticketSummaryResponse;

    @BeforeEach
    void setUp() {
        ticketSummaryResponse = new TicketSummaryResponse(
                "AutoShow 2026",
                "2026-09-01",
                "ABC123DEF456",
                TicketStatus.ACTIVE,
                new BigDecimal("55.00"),
                42L
        );
    }

    // -------------------------------------------------------------------------
    // Method getAllUserTickets()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /tickets — getAllUserTickets()")
    class GetAllUserTickets {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 200 with page of tickets when user is authenticated")
        void getAllUserTickets_shouldReturn200WithPageOfTickets_whenUserIsAuthenticated() throws Exception {
            // Given
            Page<TicketSummaryResponse> page = new PageImpl<>(
                    List.of(ticketSummaryResponse), PageRequest.of(0, 10), 1);

            when(ticketService.getUserTickets(any(), any())).thenReturn(page);

            // When / Then
            mockMvc.perform(get("/tickets")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content[0].code").value("ABC123DEF456"))
                    .andExpect(jsonPath("$.content[0].eventName").value("AutoShow 2026"))
                    .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$.content[0].price").value(55.00))
                    .andExpect(jsonPath("$.content[0].id").value(42))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 200 with empty page when user has no tickets")
        void getAllUserTickets_shouldReturn200WithEmptyPage_whenUserHasNoTickets() throws Exception {
            // Given
            when(ticketService.getUserTickets(any(), any())).thenReturn(Page.empty());

            // When / Then
            mockMvc.perform(get("/tickets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("should return 401 when user is not authenticated")
        void getAllUserTickets_shouldReturn401_whenUserIsNotAuthenticated() throws Exception {
            // When / Then
            mockMvc.perform(get("/tickets"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 403 when user does not have USER role")
        void getAllUserTickets_shouldReturn403_whenUserDoesNotHaveUserRole() throws Exception {
            // When / Then
            mockMvc.perform(get("/tickets"))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // Method getTicketPdf()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /tickets/{id}/pdf — getTicketPdf()")
    class GetTicketPdf {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 200 with PDF bytes and correct headers when ticket belongs to user")
        void getTicketPdf_shouldReturn200WithPdfBytes_whenTicketBelongsToUser() throws Exception {
            // Given
            byte[] pdfBytes = "pdf-content".getBytes();
            when(ticketService.getTicketPdf(any(), eq(42L))).thenReturn(pdfBytes);

            // When / Then
            mockMvc.perform(get("/tickets/42/pdf"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string("Content-Disposition", "attachment; filename=ticket.pdf"))
                    .andExpect(content().bytes(pdfBytes));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 404 when ticket does not exist or does not belong to user")
        void getTicketPdf_shouldReturn404_whenTicketNotFound() throws Exception {
            // Given
            when(ticketService.getTicketPdf(any(), eq(999L)))
                    .thenThrow(new TicketNotFoundException("Ticket with id 999 is not found"));

            // When / Then
            mockMvc.perform(get("/tickets/999/pdf"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 401 when user is not authenticated")
        void getTicketPdf_shouldReturn401_whenUserIsNotAuthenticated() throws Exception {
            // When / Then
            mockMvc.perform(get("/tickets/42/pdf"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 403 when user does not have USER role")
        void getTicketPdf_shouldReturn403_whenUserDoesNotHaveUserRole() throws Exception {
            // When / Then
            mockMvc.perform(get("/tickets/42/pdf"))
                    .andExpect(status().isForbidden());
        }
    }
}
