package com.ardom.automotive_event_api.common.seed;

import com.ardom.automotive_event_api.event.Event;
import com.ardom.automotive_event_api.event.EventLocation;
import com.ardom.automotive_event_api.event.EventRepository;
import com.ardom.automotive_event_api.event.EventStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("dev-seed")
@RequiredArgsConstructor
public class EventDataSeeder implements CommandLineRunner {

    private final EventRepository eventRepository;

    @Transactional
    @Override
    public void run(String... args) {
        List<Event> events = List.of(
                Event.builder()
                        .name("Spring Track Day 2027")
                        .description("A full-day track event for sports cars and enthusiasts. Includes timed sessions, instructor guidance, and networking opportunities.")
                        .location(new EventLocation(
                                "Tor Poznań",
                                "ul. Wyścigowa 3",
                                "Poznań",
                                "Poland",
                                new BigDecimal("52.406374"),
                                new BigDecimal("16.925168")
                        ))
                        .dateStart(LocalDateTime.of(2027, 4, 17, 9, 0))
                        .dateEnd(LocalDateTime.of(2027, 4, 17, 18, 0))
                        .ticketsCapacity(120)
                        .ticketPrice(new BigDecimal("299.99"))
                        .applicationFee(new BigDecimal("25.00"))
                        .status(EventStatus.PUBLISHED)
                        .build(),

                Event.builder()
                        .name("Classic Cars Rally")
                        .description("Weekend rally for classic and vintage vehicles featuring scenic routes, competitions, and vehicle showcases.")
                        .location(new EventLocation(
                                "Old Town Square",
                                "Rynek Główny",
                                "Kraków",
                                "Poland",
                                new BigDecimal("50.061430"),
                                new BigDecimal("19.936580")
                        ))
                        .dateStart(LocalDateTime.of(2027, 5, 22, 10, 0))
                        .dateEnd(LocalDateTime.of(2027, 5, 23, 16, 0))
                        .ticketsCapacity(80)
                        .ticketPrice(new BigDecimal("149.99"))
                        .applicationFee(new BigDecimal("15.00"))
                        .status(EventStatus.DRAFT)
                        .build(),

                Event.builder()
                        .name("Electric Vehicle Expo")
                        .description("An exhibition dedicated to electric mobility, charging technologies, and sustainable transportation solutions.")
                        .location(new EventLocation(
                                "Expo Center",
                                "ul. Przemysłowa 15",
                                "Warsaw",
                                "Poland",
                                new BigDecimal("52.229676"),
                                new BigDecimal("21.012229")
                        ))
                        .dateStart(LocalDateTime.of(2027, 6, 12, 8, 30))
                        .dateEnd(LocalDateTime.of(2027, 6, 13, 17, 0))
                        .ticketsCapacity(500)
                        .ticketPrice(new BigDecimal("49.99"))
                        .applicationFee(new BigDecimal("5.00"))
                        .status(EventStatus.PUBLISHED)
                        .build(),

                Event.builder()
                        .name("Night Drift Challenge")
                        .description("Competitive drift event under floodlights featuring amateur and professional drivers.")
                        .location(new EventLocation(
                                "Moto Arena",
                                "ul. Sportowa 7",
                                "Łódź",
                                "Poland",
                                new BigDecimal("51.759248"),
                                new BigDecimal("19.455983")
                        ))
                        .dateStart(LocalDateTime.of(2027, 7, 10, 18, 0))
                        .dateEnd(LocalDateTime.of(2027, 7, 11, 1, 0))
                        .ticketsCapacity(300)
                        .ticketPrice(new BigDecimal("89.99"))
                        .applicationFee(new BigDecimal("10.00"))
                        .status(EventStatus.UNPUBLISHED)
                        .build(),

                Event.builder()
                        .name("Autumn Supercar Gathering")
                        .description("Exclusive gathering of supercar owners featuring exhibitions, networking, and a scenic driving route.")
                        .location(new EventLocation(
                                "Business Park",
                                "ul. Centralna 25",
                                "Wrocław",
                                "Poland",
                                new BigDecimal("51.107883"),
                                new BigDecimal("17.038538")
                        ))
                        .dateStart(LocalDateTime.of(2026, 9, 20, 11, 0))
                        .dateEnd(LocalDateTime.of(2026, 9, 20, 19, 0))
                        .ticketsCapacity(150)
                        .ticketPrice(new BigDecimal("199.99"))
                        .applicationFee(new BigDecimal("20.00"))
                        .status(EventStatus.ARCHIVED)
                        .build()
        );

        eventRepository.deleteAllByNameIn(
                events.stream()
                        .map(Event::getName)
                        .toList());

        eventRepository.saveAll(events);
    }
}
