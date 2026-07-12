package com.ardom.automotive_event_api.common.pdf;

import com.ardom.automotive_event_api.application.Application;
import com.ardom.automotive_event_api.application.car.Car;
import com.ardom.automotive_event_api.application.payment.ApplicationPayment;
import com.ardom.automotive_event_api.ticket.Ticket;
import com.ardom.automotive_event_api.user.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PdfService {

    public byte[] generateTicketPdf(Ticket ticket) {
        return new TicketPdfGenerator(ticket).generate();
    }

    public byte[] generateApplicationPdf(ApplicationPayment payment,
                                         Application application,
                                         User user,
                                         List<Car> cars,
                                         String eventName) {
        return new ApplicationPdfGenerator(payment, application, user, cars, eventName).generate();
    }
}
