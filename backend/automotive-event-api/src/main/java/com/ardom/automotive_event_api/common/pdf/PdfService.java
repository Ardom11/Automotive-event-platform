package com.ardom.automotive_event_api.common.pdf;

import com.ardom.automotive_event_api.common.notification.ApplicationPaymentNotification;
import com.ardom.automotive_event_api.common.notification.TicketData;
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
