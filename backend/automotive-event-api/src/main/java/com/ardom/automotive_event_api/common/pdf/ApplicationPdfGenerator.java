package com.ardom.automotive_event_api.common.pdf;

import com.ardom.automotive_event_api.application.Application;
import com.ardom.automotive_event_api.application.car.Car;
import com.ardom.automotive_event_api.application.payment.ApplicationPayment;
import com.ardom.automotive_event_api.event.Event;
import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

import java.util.List;

public class ApplicationPdfGenerator {

    private final ApplicationPayment payment;
    private final Document document;
    private final List<Car> cars;

    public ApplicationPdfGenerator(ApplicationPayment payment, List<Car> cars) {
        this.payment = payment;
        this.document = new Document(PageSize.A4);
        this.cars = cars;
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
        Application app = payment.getApplication();
        Event event = app.getEvent();

        document.add(new Paragraph("Event & Application"));
        document.add(new Paragraph("Event: " + event.getName()));
        document.add(new Paragraph("Application ID: " + app.getId()));
        document.add(new Paragraph("Status: " + app.getStatus()));
        document.add(new Paragraph("\n"));
    }

    private void addCarDetails() {
        cars.forEach(car -> {
            document.add(new Paragraph("Vehicle Details"));
            document.add(new Paragraph("Brand: " + car.getBrand()));
            document.add(new Paragraph("Model: " + car.getModel()));
            document.add(new Paragraph("Year: " + car.getYear()));
            document.add(new Paragraph("\n"));
        });
    }

    private void addPaymentDetails() {
        Paragraph title = new Paragraph("Payment Information");
        title.setFont(new Font(Font.HELVETICA, 12, Font.BOLD));
        document.add(title);

        document.add(new Paragraph("Amount Paid: €" +
                payment.getAmountPaid().setScale(2, RoundingMode.HALF_UP)));
        document.add(new Paragraph("Payment Date: " +
                payment.getPaidAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"))));
        document.add(new Paragraph("Status: " + payment.getStatus()));

        document.add(new Paragraph("\n"));
        Paragraph footer = new Paragraph("Thank you for your participation!");
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }
}
