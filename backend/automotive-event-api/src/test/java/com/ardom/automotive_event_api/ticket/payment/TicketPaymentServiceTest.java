package com.ardom.automotive_event_api.ticket.payment;

import com.ardom.automotive_event_api.application.exception.TicketPaymentNotFoundException;
import com.ardom.automotive_event_api.common.email.EmailService;
import com.ardom.automotive_event_api.event.Event;
import com.ardom.automotive_event_api.event.EventRepository;
import com.ardom.automotive_event_api.event.EventStatus;
import com.ardom.automotive_event_api.event.exception.EventNotFoundException;
import com.ardom.automotive_event_api.event.exception.EventSoldOutException;
import com.ardom.automotive_event_api.event.exception.NotEnoughEventTicketsException;
import com.ardom.automotive_event_api.payment.PaymentStatus;
import com.ardom.automotive_event_api.payment.dto.response.CheckoutResponse;
import com.ardom.automotive_event_api.ticket.Ticket;
import com.ardom.automotive_event_api.ticket.TicketRepository;
import com.ardom.automotive_event_api.ticket.TicketStatus;
import com.ardom.automotive_event_api.ticket.dto.request.TicketPurchaseGuestRequest;
import com.ardom.automotive_event_api.ticket.dto.request.TicketPurchaseRequest;
import com.ardom.automotive_event_api.user.Role;
import com.ardom.automotive_event_api.user.User;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketPaymentServiceTest {

    @Mock
    private TicketPaymentRepository ticketPaymentRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private Authentication authentication;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private TicketPaymentService ticketPaymentService;

    private Event event;
    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ticketPaymentService, "baseUrl", "http://localhost:3000");
        event = Event.builder()
                .id(1L)
                .name("AutoShow 2026")
                .ticketPrice(new BigDecimal("55.00"))
                .ticketsCapacity(100)
                .status(EventStatus.PUBLISHED)
                .build();

        user = User.builder()
                .id(1L)
                .email("john@example.com")
                .role(Role.USER)
                .build();
    }

    // -------------------------------------------------------------------------
    // Method initiateCheckout(TicketPurchaseRequest, User)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("initiateCheckout() — authenticated user")
    class InitiateCheckoutUser {

        @Test
        @DisplayName("should return checkout URL when event is valid and user is authenticated")
        void initiateCheckout_shouldReturnCheckoutUrl_whenEventIsValidAndUserAuthenticated()
                throws StripeException {
            // Given
            TicketPurchaseRequest request = new TicketPurchaseRequest(1L, 2);

            when(authentication.getPrincipal()).thenReturn(user);
            when(eventRepository.findByIdAndStatus(1L, EventStatus.PUBLISHED))
                    .thenReturn(Optional.of(event));
            when(ticketRepository.countByEventId(1L)).thenReturn(0);

            TicketPayment savedPayment = TicketPayment.builder()
                    .event(event)
                    .user(user)
                    .quantity(2)
                    .amountPaid(new BigDecimal("110.00"))
                    .status(PaymentStatus.PENDING)
                    .build();
            ReflectionTestUtils.setField(savedPayment, "id", 10L);

            when(ticketPaymentRepository.save(any(TicketPayment.class))).thenReturn(savedPayment);

            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_abc123");
            when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_abc123");

            try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
                sessionStatic.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenReturn(mockSession);

                // When
                CheckoutResponse response = ticketPaymentService.initiateCheckout(authentication, request);

                // Then
                assertThat(response.checkoutUrl()).isEqualTo("https://checkout.stripe.com/pay/cs_test_abc123");
                verify(ticketPaymentRepository, times(2)).save(any(TicketPayment.class));
            }
        }

        @Test
        @DisplayName("should set amountPaid as price times quantity")
        void initiateCheckout_shouldSetCorrectAmountPaid_whenQuantityIsMoreThanOne()
                throws StripeException {
            // Given
            TicketPurchaseRequest request = new TicketPurchaseRequest(1L, 3);

            when(authentication.getPrincipal()).thenReturn(user);
            when(eventRepository.findByIdAndStatus(1L, EventStatus.PUBLISHED))
                    .thenReturn(Optional.of(event));
            when(ticketRepository.countByEventId(1L)).thenReturn(0);

            ArgumentCaptor<TicketPayment> paymentCaptor = ArgumentCaptor.forClass(TicketPayment.class);

            TicketPayment savedPayment = TicketPayment.builder().build();
            ReflectionTestUtils.setField(savedPayment, "id", 10L);
            when(ticketPaymentRepository.save(any())).thenReturn(savedPayment);

            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_abc");
            when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_abc");

            try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
                sessionStatic.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenReturn(mockSession);

                // When
                ticketPaymentService.initiateCheckout(authentication, request);

                // Then
                verify(ticketPaymentRepository, atLeastOnce()).save(paymentCaptor.capture());
                TicketPayment captured = paymentCaptor.getAllValues().getFirst();
                assertThat(captured.getAmountPaid()).isEqualByComparingTo(new BigDecimal("165.00"));
                assertThat(captured.getQuantity()).isEqualTo(3);
                assertThat(captured.getStatus()).isEqualTo(PaymentStatus.PENDING);
            }
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event does not exist or is not published")
        void initiateCheckout_shouldThrowEventNotFoundException_whenEventNotFound() {
            // Given
            TicketPurchaseRequest request = new TicketPurchaseRequest(99L, 1);

            when(authentication.getPrincipal()).thenReturn(user);
            when(eventRepository.findByIdAndStatus(99L, EventStatus.PUBLISHED))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> ticketPaymentService.initiateCheckout(authentication, request))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("should throw EventSoldOutException when event has no tickets left")
        void initiateCheckout_shouldThrowEventSoldOutException_whenEventIsSoldOut() {
            // Given
            TicketPurchaseRequest request = new TicketPurchaseRequest(1L, 1);

            when(authentication.getPrincipal()).thenReturn(user);
            when(eventRepository.findByIdAndStatus(1L, EventStatus.PUBLISHED))
                    .thenReturn(Optional.of(event));
            when(ticketRepository.countByEventId(1L)).thenReturn(100);

            // When / Then
            assertThatThrownBy(() -> ticketPaymentService.initiateCheckout(authentication, request))
                    .isInstanceOf(EventSoldOutException.class);
        }

        @Test
        @DisplayName("should throw NotEnoughEventTicketsException when requested quantity exceeds remaining")
        void initiateCheckout_shouldThrowNotEnoughEventTicketsException_whenNotEnoughTicketsLeft() {
            // Given
            TicketPurchaseRequest request = new TicketPurchaseRequest(1L, 10);

            when(authentication.getPrincipal()).thenReturn(user);
            when(eventRepository.findByIdAndStatus(1L, EventStatus.PUBLISHED))
                    .thenReturn(Optional.of(event));
            when(ticketRepository.countByEventId(1L)).thenReturn(95);

            // When / Then
            assertThatThrownBy(() -> ticketPaymentService.initiateCheckout(authentication, request))
                    .isInstanceOf(NotEnoughEventTicketsException.class)
                    .hasMessageContaining("5");
        }
    }

    // -------------------------------------------------------------------------
    // Method initiateCheckout(TicketPurchaseGuestRequest)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("initiateCheckout() — guest")
    class InitiateCheckoutGuest {

        @Test
        @DisplayName("should return checkout URL when event is valid and guest data is provided")
        void initiateCheckout_shouldReturnCheckoutUrl_whenGuestDataIsValid() throws StripeException {
            // Given
            TicketPurchaseGuestRequest request = new TicketPurchaseGuestRequest(
                    1L, 1, "Jane", "Doe", "jane@example.com");

            when(eventRepository.findByIdAndStatus(1L, EventStatus.PUBLISHED))
                    .thenReturn(Optional.of(event));
            when(ticketRepository.countByEventId(1L)).thenReturn(0);

            TicketPayment savedPayment = TicketPayment.builder().build();
            ReflectionTestUtils.setField(savedPayment, "id", 20L);
            when(ticketPaymentRepository.save(any())).thenReturn(savedPayment);

            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_guest");
            when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_guest");

            try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
                sessionStatic.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenReturn(mockSession);

                // When
                CheckoutResponse response = ticketPaymentService.initiateCheckout(request);

                // Then
                assertThat(response.checkoutUrl()).isEqualTo("https://checkout.stripe.com/pay/cs_test_guest");
            }
        }

        @Test
        @DisplayName("should persist guest fields on payment entity")
        void initiateCheckout_shouldPersistGuestFields_whenGuestCheckout() throws StripeException {
            // Given
            TicketPurchaseGuestRequest request = new TicketPurchaseGuestRequest(
                    1L, 1, "Jane", "Doe", "jane@example.com");

            when(eventRepository.findByIdAndStatus(1L, EventStatus.PUBLISHED))
                    .thenReturn(Optional.of(event));
            when(ticketRepository.countByEventId(1L)).thenReturn(0);

            ArgumentCaptor<TicketPayment> paymentCaptor = ArgumentCaptor.forClass(TicketPayment.class);

            TicketPayment savedPayment = TicketPayment.builder().build();
            ReflectionTestUtils.setField(savedPayment, "id", 20L);
            when(ticketPaymentRepository.save(any())).thenReturn(savedPayment);

            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_guest");
            when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_guest");

            try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
                sessionStatic.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenReturn(mockSession);

                // When
                ticketPaymentService.initiateCheckout(request);

                // Then
                verify(ticketPaymentRepository, atLeastOnce()).save(paymentCaptor.capture());
                TicketPayment captured = paymentCaptor.getAllValues().getFirst();
                assertThat(captured.getGuestName()).isEqualTo("Jane");
                assertThat(captured.getGuestSurname()).isEqualTo("Doe");
                assertThat(captured.getGuestEmail()).isEqualTo("jane@example.com");
                assertThat(captured.getUser()).isNull();
            }
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event does not exist")
        void initiateCheckout_shouldThrowEventNotFoundException_whenEventNotFound() {
            // Given
            TicketPurchaseGuestRequest request = new TicketPurchaseGuestRequest(
                    99L, 1, "Jane", "Doe", "jane@example.com");

            when(eventRepository.findByIdAndStatus(99L, EventStatus.PUBLISHED))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> ticketPaymentService.initiateCheckout(request))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Method handleSuccessfulCheckout()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("handleSuccessfulCheckout()")
    class HandleSuccessfulCheckout {

        @Test
        @DisplayName("should create tickets and send email when payment is pending")
        void handleSuccessfulCheckout_shouldCreateTicketsAndSendEmail_whenPaymentIsPending() {
            // Given
            TicketPayment payment = TicketPayment.builder()
                    .event(event)
                    .user(user)
                    .quantity(2)
                    .amountPaid(new BigDecimal("110.00"))
                    .status(PaymentStatus.PENDING)
                    .build();

            when(ticketPaymentRepository.findById(10L)).thenReturn(Optional.of(payment));

            // When
            ticketPaymentService.handleSuccessfulCheckout(10L);

            // Then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
            assertThat(payment.getPaidAt()).isNotNull();

            ArgumentCaptor<List<Ticket>> ticketCaptor = ArgumentCaptor.forClass(List.class);
            verify(ticketRepository).saveAll(ticketCaptor.capture());

            List<Ticket> savedTickets = ticketCaptor.getValue();
            assertThat(savedTickets).hasSize(2);
            savedTickets.forEach(ticket -> {
                assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ACTIVE);
                assertThat(ticket.getCode()).isNotBlank();
                assertThat(ticket.getPrice()).isEqualByComparingTo(new BigDecimal("55.00"));
                assertThat(ticket.getUser()).isEqualTo(user);
                assertThat(ticket.getEvent()).isEqualTo(event);
            });

            verify(emailService).sendTicket(anyList(), eq("john@example.com"), eq("AutoShow 2026"));
        }

        @Test
        @DisplayName("should send email to guest email when payment has no user")
        void handleSuccessfulCheckout_shouldSendEmailToGuestEmail_whenPaymentIsGuest() {
            // Given
            TicketPayment payment = TicketPayment.builder()
                    .event(event)
                    .guestEmail("jane@example.com")
                    .quantity(1)
                    .amountPaid(new BigDecimal("55.00"))
                    .status(PaymentStatus.PENDING)
                    .build();

            when(ticketPaymentRepository.findById(20L)).thenReturn(Optional.of(payment));

            // When
            ticketPaymentService.handleSuccessfulCheckout(20L);

            // Then
            verify(emailService).sendTicket(anyList(), eq("jane@example.com"), eq("AutoShow 2026"));
        }

        @Test
        @DisplayName("should be idempotent and skip processing when payment is already succeeded")
        void handleSuccessfulCheckout_shouldSkipProcessing_whenPaymentAlreadySucceeded() {
            // Given
            TicketPayment payment = TicketPayment.builder()
                    .event(event)
                    .user(user)
                    .quantity(1)
                    .status(PaymentStatus.SUCCEEDED)
                    .build();
            payment.setPaidAt(LocalDateTime.now().minusMinutes(5));

            when(ticketPaymentRepository.findById(10L)).thenReturn(Optional.of(payment));

            // When
            ticketPaymentService.handleSuccessfulCheckout(10L);

            // Then
            verify(ticketRepository, never()).saveAll(any());
            verify(emailService, never()).sendTicket(any(), any(), any());
        }

        @Test
        @DisplayName("should generate unique codes for each ticket")
        void handleSuccessfulCheckout_shouldGenerateUniqueCodesForTickets_whenQuantityIsMultiple() {
            // Given
            TicketPayment payment = TicketPayment.builder()
                    .event(event)
                    .user(user)
                    .quantity(3)
                    .amountPaid(new BigDecimal("165.00"))
                    .status(PaymentStatus.PENDING)
                    .build();

            when(ticketPaymentRepository.findById(10L)).thenReturn(Optional.of(payment));

            // When
            ticketPaymentService.handleSuccessfulCheckout(10L);

            // Then
            ArgumentCaptor<List<Ticket>> captor = ArgumentCaptor.forClass(List.class);
            verify(ticketRepository).saveAll(captor.capture());

            List<String> codes = captor.getValue().stream().map(Ticket::getCode).toList();
            assertThat(codes).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("should throw TicketPaymentNotFoundException when payment does not exist")
        void handleSuccessfulCheckout_shouldThrowTicketPaymentNotFoundException_whenPaymentNotFound() {
            // Given
            when(ticketPaymentRepository.findById(999L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> ticketPaymentService.handleSuccessfulCheckout(999L))
                    .isInstanceOf(TicketPaymentNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Method handleFailedCheckout()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("handleFailedCheckout()")
    class HandleFailedCheckout {

        @Test
        @DisplayName("should set status to FAILED when payment is pending")
        void handleFailedCheckout_shouldSetStatusToFailed_whenPaymentIsPending() {
            // Given
            TicketPayment payment = TicketPayment.builder()
                    .status(PaymentStatus.PENDING)
                    .build();

            when(ticketPaymentRepository.findById(10L)).thenReturn(Optional.of(payment));

            // When
            ticketPaymentService.handleFailedCheckout(10L);

            // Then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("should not change status when payment is already failed")
        void handleFailedCheckout_shouldNotChangeStatus_whenPaymentAlreadyFailed() {
            // Given
            TicketPayment payment = TicketPayment.builder()
                    .status(PaymentStatus.FAILED)
                    .build();

            when(ticketPaymentRepository.findById(10L)).thenReturn(Optional.of(payment));

            // When
            ticketPaymentService.handleFailedCheckout(10L);

            // Then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("should throw TicketPaymentNotFoundException when payment does not exist")
        void handleFailedCheckout_shouldThrowTicketPaymentNotFoundException_whenPaymentNotFound() {
            // Given
            when(ticketPaymentRepository.findById(999L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> ticketPaymentService.handleFailedCheckout(999L))
                    .isInstanceOf(TicketPaymentNotFoundException.class);
        }
    }
}
