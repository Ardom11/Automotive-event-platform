package com.ardom.automotive_event_api.common.pdf;

import com.ardom.automotive_event_api.common.notification.ApplicationPaymentNotification;
import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

public class ApplicationPdfGenerator {

    private final ApplicationPaymentNotification notification;
    private final Document document;

    public ApplicationPdfGenerator(ApplicationPaymentNotification notification) {

        this.notification = notification;
        this.document = new Document(PageSize.A4);
    }

    public byte[] generate() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader();
            addApplicationDetails();
            addCarDetails();
            addPaymentDetails();

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void addHeader() {
        Paragraph title = new Paragraph("Payment Receipt");
        title.setFont(new Font(Font.HELVETICA, 24, Font.BOLD));
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("\n"));
    }

    private void addApplicationDetails() {
        document.add(new Paragraph(String.format("%s %s\n", notification.applicantName(), notification.applicantSurname())));
        document.add(new Paragraph("Event & Application"));
        document.add(new Paragraph("Event: " + notification.eventName()));
        document.add(new Paragraph("Application ID: " + notification.applicationId()));
        document.add(new Paragraph("Status: " + notification.status()));
        document.add(new Paragraph("\n"));
    }

    private void addCarDetails() {
        notification.cars().forEach(car -> {
            document.add(new Paragraph("Vehicle Details"));
            document.add(new Paragraph("Brand: " + car.brand()));
            document.add(new Paragraph("Model: " + car.model()));
            document.add(new Paragraph("Year: " + car.year()));
            document.add(new Paragraph("\n"));
        });
    }

    private void addPaymentDetails() {
        Paragraph title = new Paragraph("Payment Information");
        title.setFont(new Font(Font.HELVETICA, 12, Font.BOLD));
        document.add(title);

        document.add(new Paragraph("Amount Paid: €" +
                notification.paymentData().amountPaid().setScale(2, RoundingMode.HALF_UP)));
        document.add(new Paragraph("Payment Date: " +
                notification.paymentData().paidAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"))));
        document.add(new Paragraph("Status: " + notification.paymentData().status()));

        document.add(new Paragraph("\n"));
        Paragraph footer = new Paragraph("Thank you for your participation!");
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }
}
