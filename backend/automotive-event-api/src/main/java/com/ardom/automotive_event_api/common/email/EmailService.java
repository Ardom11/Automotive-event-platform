package com.ardom.automotive_event_api.common.email;

import com.ardom.automotive_event_api.application.Application;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Async
    public void sendApplicationReceived(Application application) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(application.getUser().getEmail());
            message.setSubject("Application status changes");
            message.setText(String.format("We've received your application for event %s.\n" +
                            "Our team will review it and you'll get notified about its status.\n\n" +
                            "Thanks,\nBest regards",
                    application.getEvent().getName()));

            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send application received email for application {}", application.getId(), e);
        }
    }

    @Async
    public void sendApplicationApproved(Application application) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(application.getUser().getEmail());
            message.setSubject("Your application has been approved!");
            message.setText(String.format(
                    "Great news! Your application for event %s has been approved.\n" +
                            "To complete the process, please submit payment by %s.\n\n" +
                            "Thanks,\nBest regards",
                    application.getEvent().getName(),
                    application.getEvent().getPaymentDeadline()
            ));
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send application approved email for application {}", application.getId(), e);
        }
    }

    @Async
    public void sendApplicationRejected(Application application) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(application.getUser().getEmail());
            message.setSubject("Update on your application");
            message.setText(String.format(
                    "After careful review, we're unable to approve your application for event %s at this time.\n" +
                            "Reason: %s\n\n" +
                            "If you have questions, feel free to reach out.\n\n" +
                            "Thanks,\nThe Applications Team",
                    application.getEvent().getName(),
                    application.getRejectionReason()
            ));
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send application rejected email for application {}", application.getId(), e);
        }
    }

    @Async
    public void sendApplicationExpired(Application application) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(application.getUser().getEmail());
            message.setSubject("Your application has expired");
            message.setText(String.format(
                    "We inform you that your application for event %s has expired because payment was not received in time.\n\n" +
                            "Thanks,\nThe Applications Team",
                    application.getEvent().getName()
            ));
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send application expired email for application {}", application.getId(), e);
        }
    }

    @Async
    public void sendPaymentConfirmed(Application application) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(application.getUser().getEmail());
            message.setSubject("Payment confirmed");
            message.setText(String.format(
                    "We've received your payment for application to %s event. You're all set!\n\n" +
                            "Thanks,\nThe Applications Team",
                    application.getEvent().getName()
            ));
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send payment confirmed email for application {}", application.getId(), e);
        }
    }
}
