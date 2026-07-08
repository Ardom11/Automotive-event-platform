package com.ardom.automotive_event_api.application.payment;

import com.ardom.automotive_event_api.application.Application;
import com.ardom.automotive_event_api.application.ApplicationRepository;
import com.ardom.automotive_event_api.application.ApplicationStatus;
import com.ardom.automotive_event_api.application.exception.ApplicationNotFoundException;
import com.ardom.automotive_event_api.payment.PaymentStatus;
import com.ardom.automotive_event_api.payment.dto.response.CheckoutResponse;
import com.ardom.automotive_event_api.payment.exception.PaymentAlreadyInitiatedException;
import com.ardom.automotive_event_api.user.User;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ApplicationPaymentService {
    private final ApplicationPaymentRepository applicationPaymentRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional
    public CheckoutResponse initiateCheckout(Long applicationId, User user) throws StripeException {
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

    private Application getValidApplication(Long applicationId, User user) {
        Application application = applicationRepository.findByIdAndStatusAndUser(applicationId, ApplicationStatus.APPROVED_WAITING_PAYMENT, user)
                .orElseThrow(() -> new ApplicationNotFoundException("Application with id " + applicationId + " not found"));

        if (applicationPaymentRepository.existsByApplicationId(applicationId)) {
            throw new PaymentAlreadyInitiatedException("Payment already initiated for this application.");
        }

        return application;
    }

    private Session createSession(Application application, Long paymentId, String email) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://localhost:8080/applications")
                .setCancelUrl(String.format("https://localhost:8080/applications/%s", application.getId()))
                .putMetadata("type", "APPLICATION")
                .putMetadata("applicationPaymentId", paymentId.toString())
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
