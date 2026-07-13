package com.ardom.automotive_event_api.application.payment;

import com.ardom.automotive_event_api.application.Application;
import com.ardom.automotive_event_api.application.ApplicationRepository;
import com.ardom.automotive_event_api.application.ApplicationStatus;
import com.ardom.automotive_event_api.application.exception.ApplicationNotFoundException;
import com.ardom.automotive_event_api.application.exception.ApplicationPaymentNotFoundException;
import com.ardom.automotive_event_api.common.email.EmailService;
import com.ardom.automotive_event_api.event.Event;
import com.ardom.automotive_event_api.payment.PaymentStatus;
import com.ardom.automotive_event_api.payment.dto.response.CheckoutResponse;
import com.ardom.automotive_event_api.payment.exception.PaymentAlreadyInitiatedException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationPaymentServiceTest {

    @Mock
    private ApplicationPaymentRepository applicationPaymentRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private Authentication authentication;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private ApplicationPaymentService applicationPaymentService;

    private User user;
    private Event event;
    private Application application;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(applicationPaymentService, "baseUrl", "http://localhost:3000");

        user = User.builder()
                .id(1L)
                .email("john@example.com")
                .role(Role.USER)
                .build();

        event = Event.builder()
                .id(1L)
                .name("AutoShow 2026")
                .build();

        application = Application.builder()
                .id(5L)
                .fee(new BigDecimal("120.00"))
                .status(ApplicationStatus.APPROVED_WAITING_PAYMENT)
                .user(user)
                .event(event)
                .build();
    }

    // -------------------------------------------------------------------------
    // Method initiateCheckout()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("initiateCheckout()")
    class InitiateCheckout {

        @Test
        @DisplayName("should return checkout URL when application is valid and no payment exists yet")
        void initiateCheckout_shouldReturnCheckoutUrl_whenApplicationIsValidAndNoPaymentExists()
                throws StripeException {
            // Given
            when(authentication.getPrincipal()).thenReturn(user);
            when(applicationRepository.findByIdAndStatusAndUser(5L, ApplicationStatus.APPROVED_WAITING_PAYMENT, user))
                    .thenReturn(Optional.of(application));
            when(applicationPaymentRepository.existsByApplicationIdAndStatusIn(
                    eq(5L), anyList())).thenReturn(false);

            ApplicationPayment savedPayment = ApplicationPayment.builder()
                    .application(application)
                    .amountPaid(new BigDecimal("120.00"))
                    .status(PaymentStatus.PENDING)
                    .build();
            ReflectionTestUtils.setField(savedPayment, "id", 10L);

            when(applicationPaymentRepository.save(any())).thenReturn(savedPayment);

            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_app123");
            when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_app123");

            try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
                sessionStatic.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenReturn(mockSession);

                // When
                CheckoutResponse response = applicationPaymentService.initiateCheckout(authentication, 5L);

                // Then
                assertThat(response.checkoutUrl()).isEqualTo("https://checkout.stripe.com/pay/cs_test_app123");
                verify(applicationPaymentRepository, times(2)).save(any());
            }
        }

        @Test
        @DisplayName("should persist payment with PENDING status and correct amount")
        void initiateCheckout_shouldPersistPaymentWithPendingStatusAndCorrectAmount()
                throws StripeException {
            // Given
            when(authentication.getPrincipal()).thenReturn(user);
            when(applicationRepository.findByIdAndStatusAndUser(5L, ApplicationStatus.APPROVED_WAITING_PAYMENT, user))
                    .thenReturn(Optional.of(application));
            when(applicationPaymentRepository.existsByApplicationIdAndStatusIn(
                    eq(5L), anyList())).thenReturn(false);

            ArgumentCaptor<ApplicationPayment> paymentCaptor =
                    ArgumentCaptor.forClass(ApplicationPayment.class);

            ApplicationPayment savedPayment = ApplicationPayment.builder().build();
            ReflectionTestUtils.setField(savedPayment, "id", 10L);
            when(applicationPaymentRepository.save(any())).thenReturn(savedPayment);

            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_app123");
            when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_app123");

            try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
                sessionStatic.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenReturn(mockSession);

                // When
                applicationPaymentService.initiateCheckout(authentication, 5L);

                // Then
                verify(applicationPaymentRepository, atLeastOnce()).save(paymentCaptor.capture());
                ApplicationPayment captured = paymentCaptor.getAllValues().getFirst();
                assertThat(captured.getStatus()).isEqualTo(PaymentStatus.PENDING);
                assertThat(captured.getAmountPaid()).isEqualByComparingTo(new BigDecimal("120.00"));
            }
        }

        @Test
        @DisplayName("should throw ApplicationNotFoundException when application is not found or not in APPROVED_WAITING_PAYMENT status")
        void initiateCheckout_shouldThrowApplicationNotFoundException_whenApplicationNotFound() {
            // Given
            when(authentication.getPrincipal()).thenReturn(user);
            when(applicationRepository.findByIdAndStatusAndUser(99L, ApplicationStatus.APPROVED_WAITING_PAYMENT, user))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> applicationPaymentService.initiateCheckout(authentication, 99L))
                    .isInstanceOf(ApplicationNotFoundException.class);
        }

        @Test
        @DisplayName("should throw PaymentAlreadyInitiatedException when a PENDING payment already exists")
        void initiateCheckout_shouldThrowPaymentAlreadyInitiatedException_whenPendingPaymentExists() {
            // Given
            when(authentication.getPrincipal()).thenReturn(user);
            when(applicationRepository.findByIdAndStatusAndUser(5L, ApplicationStatus.APPROVED_WAITING_PAYMENT, user))
                    .thenReturn(Optional.of(application));
            when(applicationPaymentRepository.existsByApplicationIdAndStatusIn(
                    eq(5L), anyList())).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> applicationPaymentService.initiateCheckout(authentication, 5L))
                    .isInstanceOf(PaymentAlreadyInitiatedException.class);

            verify(applicationPaymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw PaymentAlreadyInitiatedException when a SUCCEEDED payment already exists")
        void initiateCheckout_shouldThrowPaymentAlreadyInitiatedException_whenSucceededPaymentExists() {
            // Given
            when(authentication.getPrincipal()).thenReturn(user);
            when(applicationRepository.findByIdAndStatusAndUser(5L, ApplicationStatus.APPROVED_WAITING_PAYMENT, user))
                    .thenReturn(Optional.of(application));
            when(applicationPaymentRepository.existsByApplicationIdAndStatusIn(
                    eq(5L), eq(List.of(PaymentStatus.PENDING, PaymentStatus.SUCCEEDED)))).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> applicationPaymentService.initiateCheckout(authentication, 5L))
                    .isInstanceOf(PaymentAlreadyInitiatedException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Method handleSuccessfulCheckout()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("handleSuccessfulCheckout()")
    class HandleSuccessfulCheckout {

        @Test
        @DisplayName("should mark payment as SUCCEEDED and application as COMPLETED")
        void handleSuccessfulCheckout_shouldMarkPaymentSucceededAndApplicationCompleted_whenPaymentIsPending() {
            // Given
            ApplicationPayment payment = ApplicationPayment.builder()
                    .application(application)
                    .amountPaid(new BigDecimal("120.00"))
                    .status(PaymentStatus.PENDING)
                    .build();

            when(applicationPaymentRepository.findById(10L)).thenReturn(Optional.of(payment));

            // When
            applicationPaymentService.handleSuccessfulCheckout(10L);

            // Then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
            assertThat(payment.getPaidAt()).isNotNull();
            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
        }

        @Test
        @DisplayName("should send confirmation email on success")
        void handleSuccessfulCheckout_shouldSendConfirmationEmail_whenPaymentSucceeds() {
            // Given
            ApplicationPayment payment = ApplicationPayment.builder()
                    .application(application)
                    .amountPaid(new BigDecimal("120.00"))
                    .status(PaymentStatus.PENDING)
                    .build();

            when(applicationPaymentRepository.findById(10L)).thenReturn(Optional.of(payment));

            // When
            applicationPaymentService.handleSuccessfulCheckout(10L);

            // Then
            verify(emailService).sendPaymentConfirmed(
                    eq(application), eq(user), eq("AutoShow 2026"));
        }

        @Test
        @DisplayName("should be idempotent and skip processing when payment is already succeeded")
        void handleSuccessfulCheckout_shouldSkipProcessing_whenPaymentAlreadySucceeded() {
            // Given
            ApplicationPayment payment = ApplicationPayment.builder()
                    .application(application)
                    .status(PaymentStatus.SUCCEEDED)
                    .build();
            payment.setPaidAt(LocalDateTime.now().minusMinutes(5));

            when(applicationPaymentRepository.findById(10L)).thenReturn(Optional.of(payment));

            // When
            applicationPaymentService.handleSuccessfulCheckout(10L);

            // Then
            // Application status should not be touched again
            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED_WAITING_PAYMENT);
            verify(emailService, never()).sendPaymentConfirmed(any(), any(), any());
        }

        @Test
        @DisplayName("should throw ApplicationPaymentNotFoundException when payment does not exist")
        void handleSuccessfulCheckout_shouldThrowApplicationPaymentNotFoundException_whenPaymentNotFound() {
            // Given
            when(applicationPaymentRepository.findById(999L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> applicationPaymentService.handleSuccessfulCheckout(999L))
                    .isInstanceOf(ApplicationPaymentNotFoundException.class);
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
            ApplicationPayment payment = ApplicationPayment.builder()
                    .application(application)
                    .status(PaymentStatus.PENDING)
                    .build();

            when(applicationPaymentRepository.findById(10L)).thenReturn(Optional.of(payment));

            // When
            applicationPaymentService.handleFailedCheckout(10L);

            // Then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("should not change status when payment is already failed")
        void handleFailedCheckout_shouldNotChangeStatus_whenPaymentAlreadyFailed() {
            // Given
            ApplicationPayment payment = ApplicationPayment.builder()
                    .application(application)
                    .status(PaymentStatus.FAILED)
                    .build();

            when(applicationPaymentRepository.findById(10L)).thenReturn(Optional.of(payment));

            // When
            applicationPaymentService.handleFailedCheckout(10L);

            // Then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("should throw ApplicationNotFoundException when payment does not exist")
        void handleFailedCheckout_shouldThrowException_whenPaymentNotFound() {
            // Given
            when(applicationPaymentRepository.findById(999L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> applicationPaymentService.handleFailedCheckout(999L))
                    .isInstanceOf(ApplicationPaymentNotFoundException.class);
        }
    }
}
