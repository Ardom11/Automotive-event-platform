package com.ardom.notification_service.pdf;

import com.ardom.notification_service.model.ApplicationPaymentNotification;
import com.ardom.notification_service.model.TicketData;
import org.springframework.stereotype.Service;

@Service
public class PdfService {

    public byte[] generateTicketPdf(TicketData ticketData) {
        return new TicketPdfGenerator(ticketData).generate();
    }

    public byte[] generateApplicationPdf(ApplicationPaymentNotification notification) {
        return new ApplicationPdfGenerator(notification).generate();
    }
}
