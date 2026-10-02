package com.ardom.automotive_event_api.common.email;

import com.ardom.automotive_event_api.common.notification.ApplicationNotification;
import com.ardom.automotive_event_api.common.notification.ApplicationPaymentNotification;
import com.ardom.automotive_event_api.common.notification.TicketData;
import com.ardom.automotive_event_api.common.notification.TicketNotification;
import com.ardom.automotive_event_api.common.pdf.PdfService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final PdfService pdfService;

    //-------------------------------------------------------------------------------------------------
    // Application
    //-------------------------------------------------------------------------------------------------
    @Async
    public void sendApplicationReceived(ApplicationNotification notification) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(notification.applicantEmail());
            message.setSubject("Application status changes");
            message.setText(String.format("We've received your application for event %s.\n" +
                            "Our team will review it and you'll get notified about its status.\n\n" +
                            "Thanks,\nBest regards",
                    notification.eventName()));

            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send application received email for application {}", notification.applicationId(), e);
        }
    }

    @Async
    public void sendApplicationApproved(ApplicationNotification notification) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(notification.applicantEmail());
            message.setSubject("Your application has been approved!");
            message.setText(String.format(
                    "Great news! Your application for event %s has been approved.\n" +
                            "To complete the process, please submit payment by %s.\n\n" +
                            "Thanks,\nBest regards",
                    notification.eventName(),
                    notification.paymentDeadline()
            ));
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send application approved email for application {}", notification.applicationId(), e);
        }
    }

    @Async
    public void sendApplicationRejected(ApplicationNotification notification) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(notification.applicantEmail());
            message.setSubject("Update on your application");
            message.setText(String.format(
                    "After careful review, we're unable to approve your application for event %s at this time.\n" +
                            "Reason: %s\n\n" +
                            "If you have questions, feel free to reach out.\n\n" +
                            "Thanks,\nThe Applications Team",
                    notification.eventName(),
                    notification.rejectionReason()
            ));
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send application rejected email for application {}", notification.applicationId(), e);
        }
    }

    @Async
    public void sendApplicationExpired(ApplicationNotification notification) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(notification.applicantEmail());
            message.setSubject("Your application has expired");
            message.setText(String.format(
                    "We inform you that your application for event %s has expired because payment was not received in time.\n\n" +
                            "Thanks,\nThe Applications Team",
                    notification.eventName()
            ));
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send application expired email for application {}", notification.applicationId(), e);
        }
    }

    @Async
    public void sendPaymentConfirmed(ApplicationPaymentNotification notification) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true);

            helper.setTo(notification.applicantEmail());
            helper.setSubject("Payment for application confirmed");
            helper.setText(String.format(String.format(
                    "We've received your payment for application to %s event. You're all set!\n\n" +
                            "Thanks,\nThe Applications Team",
                    notification.eventName()
            )));

            ByteArrayResource resource =
                    new ByteArrayResource(pdfService.generateApplicationPdf(notification));

            helper.addAttachment(
                    "payment.pdf",
                    resource,
                    "application/pdf");

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error("Failed to send payment confirmed email for application {}", notification.applicationId(), e);
        }
    }

    //-------------------------------------------------------------------------------------------------
    // Ticket
    //-------------------------------------------------------------------------------------------------
    @Async
    public void sendTicket(TicketNotification notification) {

        if (notification.tickets() == null || notification.tickets().isEmpty()) {
            log.warn("No tickets provided for email {}", notification.recipientEmail());
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true);

            helper.setTo(notification.recipientEmail());
            helper.setSubject("Ticket");
            helper.setText("""
                    Here is your ticket.
                    Thank you and see you on the event!
                    The Applications team"""
            );

            for (TicketData ticket : notification.tickets()) {

                byte[] pdfContent = pdfService.generateTicketPdf(ticket);

                helper.addAttachment(
                        ticket.ticketCode() + ".pdf",
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
