package com.ardom.automotive_event_api.common.email;

import com.ardom.automotive_event_api.application.Application;
import com.ardom.automotive_event_api.application.car.CarRepository;
import com.ardom.automotive_event_api.application.payment.ApplicationPaymentRepository;
import com.ardom.automotive_event_api.common.pdf.PdfService;
import com.ardom.automotive_event_api.ticket.Ticket;
import com.ardom.automotive_event_api.user.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final PdfService pdfService;
    private final ApplicationPaymentRepository applicationPaymentRepository;
    private final CarRepository carRepository;

    //-------------------------------------------------------------------------------------------------
    // Application
    //-------------------------------------------------------------------------------------------------
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

    //TODO
    @Async
    public void sendPaymentConfirmed(Application application, User user, String eventName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true);

            helper.setTo(user.getEmail());
            helper.setSubject("Payment for application confirmed");
            helper.setText(String.format(String.format(
                    "We've received your payment for application to %s event. You're all set!\n\n" +
                            "Thanks,\nThe Applications Team",
                    eventName
            )));

            ByteArrayResource resource =
                    new ByteArrayResource(pdfService.generateApplicationPdf(
                            applicationPaymentRepository.findByApplicationId(application.getId()),
                            application,
                            user,
                            carRepository.findAllByApplicationId(application.getId()),
                            eventName
                    ));

            helper.addAttachment(
                    "payment.pdf",
                    resource,
                    "application/pdf");

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error("Failed to send payment confirmed email for application {}", application.getId(), e);
        }
    }

    //-------------------------------------------------------------------------------------------------
    // Ticket
    //-------------------------------------------------------------------------------------------------
    @Async
    public void sendTicket(List<Ticket> tickets, String email, String eventName) {

        if (tickets == null || tickets.isEmpty()) {
            log.warn("No tickets provided for email {}", email);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true);

            helper.setTo(email);
            helper.setSubject(String.format("Your ticket for %s", eventName));
            helper.setText(String.format("Here is your ticket for %s.\n" +
                            "Thank you and see you on the event!" +
                            "\nThe Applications team",
                    eventName
            ));

            for (Ticket ticket : tickets) {

                byte[] pdfContent = pdfService.generateTicketPdf(ticket);

                helper.addAttachment(
                        ticket.getCode() + ".pdf",
                        new ByteArrayResource(pdfContent),
                        "application/pdf"
                );
            }

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error("Failed to send ticket email\n{}", e.getMessage());
        }
    }
}
