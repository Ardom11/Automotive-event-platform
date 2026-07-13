package com.ardom.automotive_event_api.application.payment;

import com.ardom.automotive_event_api.application.Application;
import com.ardom.automotive_event_api.application.ApplicationRepository;
import com.ardom.automotive_event_api.application.ApplicationStatus;
import com.ardom.automotive_event_api.application.exception.ApplicationNotFoundException;
import com.ardom.automotive_event_api.application.exception.ApplicationPaymentNotFoundException;
import com.ardom.automotive_event_api.common.email.EmailService;
import com.ardom.automotive_event_api.payment.PaymentStatus;
import com.ardom.automotive_event_api.payment.dto.response.CheckoutResponse;
import com.ardom.automotive_event_api.payment.exception.PaymentAlreadyInitiatedException;
import com.ardom.automotive_event_api.user.User;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationPaymentService {
    @Value("${application.base-url}")
    private String baseUrl;

    private final ApplicationPaymentRepository applicationPaymentRepository;
    private final ApplicationRepository applicationRepository;
    private final EmailService emailService;

    @Transactional
    public CheckoutResponse initiateCheckout(Authentication authentication, Long applicationId) throws StripeException {
        User user = (User) authentication.getPrincipal();

        Application application = getValidApplication(applicationId, user);

        ApplicationPayment payment = ApplicationPayment.builder()
                .application(application)
                .amountPaid(application.getFee())
                .status(PaymentStatus.PENDING)
                .build();

        payment = applicationPaymentRepository.save(payment);

        Session session = createSession(application, payment.getId(), user.getEmail());

        payment.setStripePaymentId(session.getId());
        applicationPaymentRepository.save(payment);

        return new CheckoutResponse(session.getUrl());
    }

    @Transactional
    public void handleSuccessfulCheckout(Long applicationPaymentId) {
        ApplicationPayment payment = applicationPaymentRepository.findById(applicationPaymentId)
                .orElseThrow(() -> new ApplicationPaymentNotFoundException("Application payment with id " + applicationPaymentId + " not found"));

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return;
        }

        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaidAt(LocalDateTime.now());

        Application application = payment.getApplication();
        application.setStatus(ApplicationStatus.COMPLETED);

        emailService.sendPaymentConfirmed(
                application,
                application.getUser(),
                application.getEvent().getName());
    }

    @Transactional
    public void handleFailedCheckout(Long referenceId) {
        ApplicationPayment payment = applicationPaymentRepository.findById(referenceId)
                .orElseThrow(() -> new ApplicationPaymentNotFoundException("Application payment with id " + referenceId + " is not found"));

        if (!payment.getStatus().equals(PaymentStatus.FAILED)) {
            payment.setStatus(PaymentStatus.FAILED);
        }
    }

    private Application getValidApplication(Long applicationId, User user) {
        Application application = applicationRepository.findByIdAndStatusAndUser(applicationId, ApplicationStatus.APPROVED_WAITING_PAYMENT, user)
                .orElseThrow(() -> new ApplicationNotFoundException("Application with id " + applicationId + " not found"));

        if (applicationPaymentRepository.existsByApplicationIdAndStatusIn(applicationId,
                List.of(
                        PaymentStatus.PENDING,
                        PaymentStatus.SUCCEEDED
                )
        )) {
            throw new PaymentAlreadyInitiatedException("Payment already initiated for this application.");
        }

        return application;
    }

    private Session createSession(Application application, Long paymentId, String email) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(baseUrl + "/applications/" + application.getId())
                .setCancelUrl(baseUrl + "/applications/")
                .putMetadata("type", "APPLICATION")
                .putMetadata("referenceId", paymentId.toString())
                .setCustomerEmail(email)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setUnitAmount(application.getFee().multiply(BigDecimal.valueOf(100)).longValueExact())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Application fee — " + application.getEvent().getName())
                                        .build())
                                .build())
                        .build())
                .build();

        return Session.create(params);
    }
}
