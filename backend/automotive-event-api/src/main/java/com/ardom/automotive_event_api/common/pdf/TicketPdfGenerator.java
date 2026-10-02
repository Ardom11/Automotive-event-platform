package com.ardom.automotive_event_api.common.pdf;

import com.ardom.automotive_event_api.common.notification.TicketData;
import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

public class TicketPdfGenerator {

    private final TicketData ticket;
    private final Document document;

    public TicketPdfGenerator(TicketData ticket) {
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
        document.add(new Paragraph("Event Details"));
        document.add(new Paragraph("Event: " + ticket.eventName()));
        document.add(new Paragraph(String.format("Date: %s - %s",
                ticket.eventDateStart().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")),
                ticket.eventDateEnd().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")))));
        document.add(new Paragraph("Place: " + ticket.locationPlace()));
        document.add(new Paragraph(String.format("%s, %s", ticket.locationCity(), ticket.locationCountry())));
        document.add(new Paragraph(ticket.locationAddress()));
        document.add(new Paragraph("\n"));
    }

    private void addTicketInfo() {
        document.add(new Paragraph("Ticket Holder"));
        document.add(new Paragraph("Name: " + ticket.holderName() + " " + ticket.holderSurname()));
        document.add(new Paragraph(String.format("Ticket №: %s\n", ticket.ticketCode())));
    }

    private void addTicketCode() {
        Paragraph code = new Paragraph("Ticket Code: " + ticket.ticketCode());
        code.setFont(new Font(Font.COURIER, 14, Font.BOLD));
        code.setAlignment(Element.ALIGN_CENTER);
        document.add(new Paragraph("\n"));
        document.add(code);
    }
}
