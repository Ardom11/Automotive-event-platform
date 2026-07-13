package com.ardom.automotive_event_api.ticket;

import com.ardom.automotive_event_api.common.pdf.PdfService;
import com.ardom.automotive_event_api.ticket.dto.response.TicketSummaryResponse;
import com.ardom.automotive_event_api.ticket.exception.TicketNotFoundException;
import com.ardom.automotive_event_api.user.Role;
import com.ardom.automotive_event_api.user.User;
import com.ardom.automotive_event_api.user.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private PdfService pdfService;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private TicketService ticketService;

    private User user;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("john@example.com")
                .role(Role.USER)
                .build();

        ticket = Ticket.builder()
                .id(42L)
                .code("ABC123DEF456")
                .status(TicketStatus.ACTIVE)
                .price(new BigDecimal("55.00"))
                .build();
    }

    // -------------------------------------------------------------------------
    // Method getUserTickets()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getUserTickets()")
    class GetUserTickets {

        @Test
        @DisplayName("should return a page of ticket responses for the authenticated user")
        void getUserTickets_shouldReturnPageOfTicketResponses_whenUserIsAuthenticated() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            TicketSummaryResponse summaryResponse = new TicketSummaryResponse(
                    "AutoShow 2026", "2026-09-01", "ABC123DEF456",
                    TicketStatus.ACTIVE, new BigDecimal("55.00"), 42L);

            Page<Ticket> ticketPage = new PageImpl<>(List.of(ticket), pageable, 1);

            when(authentication.getPrincipal()).thenReturn(user);
            when(ticketRepository.findAllByUserId(1L, pageable)).thenReturn(ticketPage);
            when(ticketMapper.toResponse(ticket)).thenReturn(summaryResponse);

            // When
            Page<TicketSummaryResponse> result = ticketService.getUserTickets(authentication, pageable);

            // Then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().getFirst().code()).isEqualTo("ABC123DEF456");
            assertThat(result.getContent().getFirst().status()).isEqualTo(TicketStatus.ACTIVE);

            verify(ticketRepository).findAllByUserId(1L, pageable);
            verify(ticketMapper).toResponse(ticket);
        }

        @Test
        @DisplayName("should return empty page when user has no tickets")
        void getUserTickets_shouldReturnEmptyPage_whenUserHasNoTickets() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            when(authentication.getPrincipal()).thenReturn(user);
            when(ticketRepository.findAllByUserId(1L, pageable))
                    .thenReturn(Page.empty(pageable));

            // When
            Page<TicketSummaryResponse> result = ticketService.getUserTickets(authentication, pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
            verify(ticketMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("should throw UserNotFoundException when principal is null")
        void getUserTickets_shouldThrowUserNotFoundException_whenPrincipalIsNull() {
            // Given
            when(authentication.getPrincipal()).thenReturn(null);

            // When / Then
            assertThatThrownBy(() -> ticketService.getUserTickets(authentication, Pageable.unpaged()))
                    .isInstanceOf(UserNotFoundException.class);

            verify(ticketRepository, never()).findAllByUserId(any(), any());
        }
    }

    // -------------------------------------------------------------------------
    // Method getTicketPdf()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getTicketPdf()")
    class GetTicketPdf {

        @Test
        @DisplayName("should return PDF bytes when ticket belongs to the authenticated user")
        void getTicketPdf_shouldReturnPdfBytes_whenTicketBelongsToUser() {
            // Given
            byte[] pdfBytes = "pdf-content".getBytes();

            when(authentication.getPrincipal()).thenReturn(user);
            when(ticketRepository.findByIdAndUserId(42L, 1L)).thenReturn(ticket);
            when(pdfService.generateTicketPdf(ticket)).thenReturn(pdfBytes);

            // When
            byte[] result = ticketService.getTicketPdf(authentication, 42L);

            // Then
            assertThat(result).isEqualTo(pdfBytes);
            verify(pdfService).generateTicketPdf(ticket);
        }

        @Test
        @DisplayName("should throw TicketNotFoundException when ticket does not belong to user")
        void getTicketPdf_shouldThrowTicketNotFoundException_whenTicketDoesNotBelongToUser() {
            // Given
            when(authentication.getPrincipal()).thenReturn(user);
            when(ticketRepository.findByIdAndUserId(42L, 1L)).thenReturn(null);

            // When / Then
            assertThatThrownBy(() -> ticketService.getTicketPdf(authentication, 42L))
                    .isInstanceOf(TicketNotFoundException.class)
                    .hasMessageContaining("42");

            verify(pdfService, never()).generateTicketPdf(any());
        }

        @Test
        @DisplayName("should throw TicketNotFoundException when ticket does not exist")
        void getTicketPdf_shouldThrowTicketNotFoundException_whenTicketNotFound() {
            // Given
            when(authentication.getPrincipal()).thenReturn(user);
            when(ticketRepository.findByIdAndUserId(999L, 1L)).thenReturn(null);

            // When / Then
            assertThatThrownBy(() -> ticketService.getTicketPdf(authentication, 999L))
                    .isInstanceOf(TicketNotFoundException.class);
        }

        @Test
        @DisplayName("should throw UserNotFoundException when principal is null")
        void getTicketPdf_shouldThrowUserNotFoundException_whenPrincipalIsNull() {
            // Given
            when(authentication.getPrincipal()).thenReturn(null);

            // When / Then
            assertThatThrownBy(() -> ticketService.getTicketPdf(authentication, 42L))
                    .isInstanceOf(UserNotFoundException.class);

            verify(ticketRepository, never()).findByIdAndUserId(any(), any());
            verify(pdfService, never()).generateTicketPdf(any());
        }
    }
}