package com.ardom.automotive_event_api.common.pdf;

import com.ardom.automotive_event_api.event.Event;
import com.ardom.automotive_event_api.event.EventLocation;
import com.ardom.automotive_event_api.ticket.Ticket;
import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

public class TicketPdfGenerator {

    private final Ticket ticket;
    private final Document document;

    public TicketPdfGenerator(Ticket ticket) {
        this.ticket = ticket;
        this.document = new Document(PageSize.A4);
    }

    public byte[] generate() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader();
            addEventDetails();
            addTicketInfo();
            addTicketCode();

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void addHeader() {
        Paragraph title = new Paragraph("Festival Ticket");
        title.setFont(new Font(Font.HELVETICA, 24, Font.BOLD));
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("\n"));
    }

    private void addEventDetails() {
        Event event = ticket.getEvent();

        document.add(new Paragraph("Event Details"));
        document.add(new Paragraph("Event: " + event.getName()));
        document.add(new Paragraph(String.format("Date: %s - %s",
                event.getDateStart().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")),
                event.getDateEnd().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")))));
        EventLocation location = event.getLocation();
        document.add(new Paragraph("Place: " + location.getPlace()));
        document.add(new Paragraph(String.format("%s, %s", location.getCity(), location.getCountry())));
        document.add(new Paragraph(location.getAddress()));
        document.add(new Paragraph("\n"));
    }

    private void addTicketInfo() {
        String holderName = ticket.getUser() != null
                ? ticket.getUser().getName() + " " + ticket.getUser().getSurname()
                : ticket.getPayment().getGuestName() + " " + ticket.getPayment().getGuestSurname();

        document.add(new Paragraph("Ticket Holder"));
        document.add(new Paragraph("Name: " + holderName));
        document.add(new Paragraph(String.format("Ticket №: %s\n", ticket.getCode())));
    }

    private void addTicketCode() {
        Paragraph code = new Paragraph("Ticket Code: " + ticket.getCode());
        code.setFont(new Font(Font.COURIER, 14, Font.BOLD));
        code.setAlignment(Element.ALIGN_CENTER);
        document.add(new Paragraph("\n"));
        document.add(code);
    }
}
