package com.ardom.automotive_event_api.common.pdf;

import com.ardom.automotive_event_api.application.car.Car;
import com.ardom.automotive_event_api.application.payment.ApplicationPayment;
import com.ardom.automotive_event_api.ticket.Ticket;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PdfService {

    public byte[] generateTicketPdf(Ticket ticket){
        return new TicketPdfGenerator(ticket).generate();
    }

    public byte[] generateApplicationPdf(ApplicationPayment payment, List<Car> cars){
        return new ApplicationPdfGenerator(payment, cars).generate();
    }
}
