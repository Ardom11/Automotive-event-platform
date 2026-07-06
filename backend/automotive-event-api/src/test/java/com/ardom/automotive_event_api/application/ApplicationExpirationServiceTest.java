package com.ardom.automotive_event_api.application;

import com.ardom.automotive_event_api.common.email.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationExpirationService")
class ApplicationExpirationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ApplicationExpirationService applicationExpirationService;

    // -------------------------------------------------------------------------
    // Method expireUnpaidApplications()
    // -------------------------------------------------------------------------
    @Nested
    class ExpireUnpaidApplications {

        @Test
        @DisplayName("Should mark all found applications as EXPIRED when applications match query")
        void expireUnpaidApplications_shouldMarkApplicationsAsExpired_whenApplicationsAreFound() {
            // given
            Application firstApplication = new Application();
            firstApplication.setStatus(ApplicationStatus.APPROVED_WAITING_PAYMENT);

            Application secondApplication = new Application();
            secondApplication.setStatus(ApplicationStatus.APPROVED_WAITING_PAYMENT);

            when(applicationRepository.findAllByStatusAndEvent_DateStartBefore(any(), any()))
                    .thenReturn(List.of(firstApplication, secondApplication));

            // when
            applicationExpirationService.expireUnpaidApplications();

            // then
            assertThat(firstApplication.getStatus()).isEqualTo(ApplicationStatus.EXPIRED);
            assertThat(secondApplication.getStatus()).isEqualTo(ApplicationStatus.EXPIRED);
        }

        @Test
        @DisplayName("Should do nothing when no applications match query")
        void expireUnpaidApplications_shouldDoNothing_whenNoApplicationsAreFound() {
            // given
            when(applicationRepository.findAllByStatusAndEvent_DateStartBefore(any(), any()))
                    .thenReturn(List.of());

            // when
            applicationExpirationService.expireUnpaidApplications();

            // then
            verify(applicationRepository).findAllByStatusAndEvent_DateStartBefore(any(), any());
            verifyNoMoreInteractions(applicationRepository);
        }

        @Test
        @DisplayName("Should query repository with APPROVED_WAITING_PAYMENT status")
        void expireUnpaidApplications_shouldQueryWithApprovedWaitingPaymentStatus_always() {
            // given
            when(applicationRepository.findAllByStatusAndEvent_DateStartBefore(any(), any()))
                    .thenReturn(List.of());

            // when
            applicationExpirationService.expireUnpaidApplications();

            // then
            verify(applicationRepository)
                    .findAllByStatusAndEvent_DateStartBefore(eq(ApplicationStatus.APPROVED_WAITING_PAYMENT), any());
        }

        @Test
        @DisplayName("Should query repository with a date 7 days from today at start of day")
        void expireUnpaidApplications_shouldQueryWithDateSevenDaysFromToday_always() {
            // given
            when(applicationRepository.findAllByStatusAndEvent_DateStartBefore(any(), any()))
                    .thenReturn(List.of());

            LocalDateTime expectedThreshold = LocalDate.now().plusDays(7).atStartOfDay();

            ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

            // when
            applicationExpirationService.expireUnpaidApplications();

            // then
            verify(applicationRepository)
                    .findAllByStatusAndEvent_DateStartBefore(eq(ApplicationStatus.APPROVED_WAITING_PAYMENT), thresholdCaptor.capture());

            assertThat(thresholdCaptor.getValue()).isEqualTo(expectedThreshold);
        }

        @Test
        @DisplayName("Should not change status of applications that are not returned by query")
        void expireUnpaidApplications_shouldNotAffectOtherApplications_whenTheyAreNotReturnedByQuery() {
            // given
            Application unrelatedApplication = new Application();
            unrelatedApplication.setStatus(ApplicationStatus.PENDING);

            when(applicationRepository.findAllByStatusAndEvent_DateStartBefore(any(), any()))
                    .thenReturn(List.of());

            // when
            applicationExpirationService.expireUnpaidApplications();

            // then
            assertThat(unrelatedApplication.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        }
    }
}
