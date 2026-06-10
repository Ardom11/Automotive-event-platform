package com.ardom.automotive_event_api.event;

import com.ardom.automotive_event_api.event.dto.LocationDto;
import com.ardom.automotive_event_api.event.dto.request.CreateEventRequest;
import com.ardom.automotive_event_api.event.dto.request.UpdateEventRequest;
import com.ardom.automotive_event_api.event.dto.response.EventResponse;
import com.ardom.automotive_event_api.event.dto.response.EventSummaryResponse;
import com.ardom.automotive_event_api.event.exception.DeletingNotDraftEventException;
import com.ardom.automotive_event_api.event.exception.EventNotFoundException;
import com.ardom.automotive_event_api.event.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService")
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    private final EventMapper eventMapper = new EventMapper();

    private EventService eventService;

    private final LocalDateTime dateStart = LocalDateTime.now().plusMonths(1);
    private final LocalDateTime dateEnd = LocalDateTime.now().plusMonths(1).plusDays(2);
    private final LocalDate applicationDeadline = dateStart.toLocalDate().minusWeeks(2);
    private final LocalDate paymentDeadline = dateStart.toLocalDate().minusDays(7);
    private Event draftEvent;
    private Event publishedEvent;
    private EventResponse eventResponse;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository, eventMapper);

        EventLocation location = new EventLocation(
                "Circuit Park", "Main Street 1", "Amsterdam", "Netherlands", new BigDecimal("52.3654"), new BigDecimal("4.9041")
        );

        draftEvent = Event.builder()
                .id(1L)
                .name("Festival1")
                .description("Very interesting automotive event")
                .location(location)
                .dateStart(dateStart)
                .dateEnd(dateEnd)
                .ticketsCapacity(3000)
                .ticketPrice(new BigDecimal("60.00"))
                .applicationFee(new BigDecimal("110.00"))
                .status(EventStatus.DRAFT)
                .build();

        publishedEvent = Event.builder()
                .id(2L)
                .name("Festival2")
                .description("Another great event")
                .location(location)
                .dateStart(dateStart)
                .dateEnd(dateEnd)
                .ticketsCapacity(2000)
                .ticketPrice(new BigDecimal("55.00"))
                .applicationFee(new BigDecimal("100.00"))
                .status(EventStatus.PUBLISHED)
                .build();

        LocationDto locationDto = new LocationDto(
                "Circuit Park", "Main Street 1", "Amsterdam", "Netherlands",
                new BigDecimal("52.3654"), new BigDecimal("4.9041")
        );

        eventResponse = new EventResponse(
                1L, "Festival1", "Very interesting automotive event",
                locationDto,
                dateStart,
                dateEnd,
                applicationDeadline, paymentDeadline,
                3000, new BigDecimal("60.00"), new BigDecimal("110.00"),
                EventStatus.DRAFT,
                null, null
        );
    }

    // -------------------------------------------------------------------------
    // Method createEvent()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createEvent")
    class CreateEvent {

        @Test
        @DisplayName("should create event and return response when request is valid")
        void createEvent_shouldCreateAndReturnResponse_whenRequestIsValid() {
            // given
            CreateEventRequest request = mock(CreateEventRequest.class);
            when(eventRepository.save(any(Event.class))).thenReturn(draftEvent);

            // when
            EventResponse result = eventService.createEvent(request);

            // then
            assertThat(result).isEqualTo(eventResponse);
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("should save exactly once when request is valid")
        void createEvent_shouldSaveExactlyOnce_whenRequestIsValid() {
            // given
            CreateEventRequest request = mock(CreateEventRequest.class);
            when(eventRepository.save(any(Event.class))).thenReturn(draftEvent);

            // when
            eventService.createEvent(request);

            // then
            verify(eventRepository, times(1)).save(any(Event.class));
        }
    }

    // -------------------------------------------------------------------------
    // Method updateEventStatus()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateEventStatus")
    class UpdateEventStatus {

        @Test
        @DisplayName("should update status and return response when transition is DRAFT -> PUBLISHED")
        void updateEventStatus_shouldUpdateStatus_whenTransitionIsDraftToPublished() {
            // given
            EventResponse expectedResponse = new EventResponse(
                    eventResponse.id(),
                    eventResponse.name(),
                    eventResponse.description(),
                    eventResponse.location(),
                    eventResponse.dateStart(),
                    eventResponse.dateEnd(),
                    eventResponse.applicationDeadline(),
                    eventResponse.paymentDeadline(),
                    eventResponse.ticketsCapacity(),
                    eventResponse.ticketPrice(),
                    eventResponse.applicationFee(),
                    EventStatus.PUBLISHED,
                    eventResponse.createdAt(),
                    eventResponse.modifiedAt());
            when(eventRepository.findById(1L)).thenReturn(Optional.of(draftEvent));
            when(eventRepository.save(draftEvent)).thenReturn(draftEvent);

            // when
            EventResponse result = eventService.updateEventStatus(1L, EventStatus.PUBLISHED);

            // then
            assertThat(result).isEqualTo(expectedResponse);
            assertThat(draftEvent.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        }

        @Test
        @DisplayName("should update status and return response when transition is PUBLISHED -> UNPUBLISHED")
        void updateEventStatus_shouldUpdateStatus_whenTransitionIsPublishedToUnpublished() {
            // given
            EventResponse expectedResponse = new EventResponse(
                    publishedEvent.getId(),
                    publishedEvent.getName(),
                    publishedEvent.getDescription(),
                    eventMapper.toLocationDto(publishedEvent.getLocation()),
                    publishedEvent.getDateStart(),
                    publishedEvent.getDateEnd(),
                    publishedEvent.getApplicationDeadline(),
                    publishedEvent.getPaymentDeadline(),
                    publishedEvent.getTicketsCapacity(),
                    publishedEvent.getTicketPrice(),
                    publishedEvent.getApplicationFee(),
                    EventStatus.UNPUBLISHED,
                    publishedEvent.getCreatedAt(),
                    publishedEvent.getCreatedAt()
            );
            when(eventRepository.findById(2L)).thenReturn(Optional.of(publishedEvent));
            when(eventRepository.save(publishedEvent)).thenReturn(publishedEvent);

            // when
            EventResponse result = eventService.updateEventStatus(2L, EventStatus.UNPUBLISHED);

            // then
            assertThat(result).isEqualTo(expectedResponse);
            assertThat(publishedEvent.getStatus()).isEqualTo(EventStatus.UNPUBLISHED);
        }

        @Test
        @DisplayName("should update status and return response when transition is PUBLISHED -> ARCHIVED")
        void updateEventStatus_shouldUpdateStatus_whenTransitionIsPublishedToArchived() {
            // given
            EventResponse expectedResponse = new EventResponse(
                    publishedEvent.getId(),
                    publishedEvent.getName(),
                    publishedEvent.getDescription(),
                    eventMapper.toLocationDto(publishedEvent.getLocation()),
                    publishedEvent.getDateStart(),
                    publishedEvent.getDateEnd(),
                    publishedEvent.getApplicationDeadline(),
                    publishedEvent.getPaymentDeadline(),
                    publishedEvent.getTicketsCapacity(),
                    publishedEvent.getTicketPrice(),
                    publishedEvent.getApplicationFee(),
                    EventStatus.ARCHIVED,
                    publishedEvent.getCreatedAt(),
                    publishedEvent.getCreatedAt()
            );
            when(eventRepository.findById(2L)).thenReturn(Optional.of(publishedEvent));
            when(eventRepository.save(publishedEvent)).thenReturn(publishedEvent);

            // when
            EventResponse result = eventService.updateEventStatus(2L, EventStatus.ARCHIVED);

            // then
            assertThat(result).isEqualTo(expectedResponse);
            assertThat(publishedEvent.getStatus()).isEqualTo(EventStatus.ARCHIVED);
        }

        @Test
        @DisplayName("should throw InvalidStatusTransitionException when transition is DRAFT -> ARCHIVED")
        void updateEventStatus_shouldThrowInvalidStatusTransitionException_whenTransitionIsDraftToArchived() {
            // given
            when(eventRepository.findById(1L)).thenReturn(Optional.of(draftEvent));

            // when / then
            assertThatThrownBy(() -> eventService.updateEventStatus(1L, EventStatus.ARCHIVED))
                    .isInstanceOf(InvalidStatusTransitionException.class);

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw InvalidStatusTransitionException when transition is ARCHIVED -> any")
        void updateEventStatus_shouldThrowInvalidStatusTransitionException_whenTransitionIsFromArchived() {
            // given
            draftEvent.setStatus(EventStatus.ARCHIVED);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(draftEvent));

            // when / then
            assertThatThrownBy(() -> eventService.updateEventStatus(1L, EventStatus.PUBLISHED))
                    .isInstanceOf(InvalidStatusTransitionException.class);

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event does not exist")
        void updateEventStatus_shouldThrowEventNotFoundException_whenEventDoesNotExist() {
            // given
            when(eventRepository.findById(99L)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> eventService.updateEventStatus(99L, EventStatus.PUBLISHED))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Method updateEvent()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateEvent")
    class UpdateEvent {

        @Test
        @DisplayName("should update name when only name is provided")
        void updateEvent_shouldUpdateName_whenOnlyNameIsProvided() {
            // given
            UpdateEventRequest request = mock(UpdateEventRequest.class);
            when(request.name()).thenReturn("New Name");

            when(eventRepository.findById(1L)).thenReturn(Optional.of(draftEvent));
            when(eventRepository.save(draftEvent)).thenReturn(draftEvent);

            // when
            eventService.updateEvent(1L, request);

            // then
            ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
            verify(eventRepository).save(captor.capture());

            Event saved = captor.getValue();

            assertThat(saved.getName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("should update location fields when location data is provided")
        void updateEvent_shouldUpdateLocation_whenLocationDataIsProvided() {
            // given
            UpdateEventRequest request = mock(UpdateEventRequest.class);

            when(request.place()).thenReturn("New Circuit");
            when(request.address()).thenReturn("New Address 5");
            when(request.city()).thenReturn("Berlin");
            when(request.country()).thenReturn("Germany");
            when(request.latitude()).thenReturn(new BigDecimal("52.52"));
            when(request.longitude()).thenReturn(new BigDecimal("13.405"));

            when(eventRepository.findById(1L)).thenReturn(Optional.of(draftEvent));
            when(eventRepository.save(draftEvent)).thenReturn(draftEvent);

            // when
            eventService.updateEvent(1L, request);

            // then
            ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
            verify(eventRepository).save(captor.capture());

            Event saved = captor.getValue();
            EventLocation location = saved.getLocation();

            assertThat(location.getPlace()).isEqualTo("New Circuit");
            assertThat(location.getCity()).isEqualTo("Berlin");
            assertThat(location.getCountry()).isEqualTo("Germany");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when new dateStart is after existing dateEnd")
        void updateEvent_shouldThrowIllegalArgumentException_whenDateStartIsAfterDateEnd() {
            // given
            LocalDateTime invalidStart = draftEvent.getDateEnd().plusDays(1);

            UpdateEventRequest request = mock(UpdateEventRequest.class);
            when(request.dateStart()).thenReturn(invalidStart);

            when(eventRepository.findById(1L)).thenReturn(Optional.of(draftEvent));

            // when / then
            assertThatThrownBy(() -> eventService.updateEvent(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date cannot be after end date");

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event does not exist")
        void updateEvent_shouldThrowEventNotFoundException_whenEventDoesNotExist() {
            // given
            UpdateEventRequest request = mock(UpdateEventRequest.class);
            when(eventRepository.findById(99L)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> eventService.updateEvent(99L, request))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Method getAllEvents()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getAllEvents")
    class GetAllEvents {

        @Test
        @DisplayName("should return page of all events when called")
        void getAllEvents_shouldReturnPageOfEvents_whenCalled() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> eventPage = new PageImpl<>(List.of(draftEvent, publishedEvent));
            when(eventRepository.findAll(pageable)).thenReturn(eventPage);

            // when
            Page<EventResponse> result = eventService.getAllEvents(pageable);

            // then
            assertThat(result).hasSize(2);
            verify(eventRepository).findAll(pageable);
        }

        @Test
        @DisplayName("should return empty page when no events exist")
        void getAllEvents_shouldReturnEmptyPage_whenNoEventsExist() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            when(eventRepository.findAll(pageable)).thenReturn(Page.empty());

            // when
            Page<EventResponse> result = eventService.getAllEvents(pageable);

            // then
            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Method getPublishedEvents()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getPublishedEvents")
    class GetPublishedEvents {

        @Test
        @DisplayName("should return only published events when called")
        void getPublishedEvents_shouldReturnOnlyPublishedEvents_whenCalled() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> publishedPage = new PageImpl<>(List.of(publishedEvent));
            when(eventRepository.findByStatus(EventStatus.PUBLISHED, pageable)).thenReturn(publishedPage);

            // when
            Page<EventSummaryResponse> result = eventService.getPublishedEvents(pageable);

            // then
            assertThat(result).hasSize(1);
            verify(eventRepository).findByStatus(EventStatus.PUBLISHED, pageable);
        }
    }

    // -------------------------------------------------------------------------
    // Method getFullEvent()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getFullEvent")
    class GetFullEvent {

        @Test
        @DisplayName("should return event response when event exists")
        void getFullEvent_shouldReturnEventResponse_whenEventExists() {
            // given
            when(eventRepository.findById(1L)).thenReturn(Optional.of(draftEvent));

            // when
            EventResponse result = eventService.getFullEvent(1L);

            // then
            assertThat(result).isEqualTo(eventResponse);
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event does not exist")
        void getFullEvent_shouldThrowEventNotFoundException_whenEventDoesNotExist() {
            // given
            when(eventRepository.findById(99L)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> eventService.getFullEvent(99L))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Method getEventSummary()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getEventSummary")
    class GetEventSummary {

        @Test
        @DisplayName("should return event summary when event exists")
        void getEventSummary_shouldReturnEventSummary_whenEventExists() {
            // given
            EventSummaryResponse expectedResponse = eventMapper.toPublicResponse(publishedEvent);
            when(eventRepository.findById(2L)).thenReturn(Optional.of(publishedEvent));

            // when
            EventSummaryResponse result = eventService.getEventSummary(2L);

            // then
            assertThat(result).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event does not exist")
        void getEventSummary_shouldThrowEventNotFoundException_whenEventDoesNotExist() {
            // given
            when(eventRepository.findById(99L)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> eventService.getEventSummary(99L))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Method deleteEvent()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("deleteEvent")
    class DeleteEvent {

        @Test
        @DisplayName("should call deleteById when event exists and with status DRAFT")
        void deleteEvent_shouldCallDeleteById_whenEventExistsAndStatusIsDRAFT() {
            // given
            when(eventRepository.findById(1L))
                    .thenReturn(Optional.of(draftEvent));

            // when
            eventService.deleteEvent(1L);

            // then
            verify(eventRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should throw DeletingNotDraftEventException when event is not in DRAFT status")
        void deleteEvent_shouldThrowDeletingNotDraftEventException_whenEventIsNotDraft() {
            // given
            when(eventRepository.findById(2L))
                    .thenReturn(Optional.of(publishedEvent));

            // when / then
            assertThatThrownBy(() -> eventService.deleteEvent(2L))
                    .isInstanceOf(DeletingNotDraftEventException.class);

            verify(eventRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event does not exist")
        void deleteEvent_shouldThrowEventNotFoundException_whenEventDoesNotExist() {
            // given
            when(eventRepository.findById(99L))
                    .thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> eventService.deleteEvent(99L))
                    .isInstanceOf(EventNotFoundException.class);

            verify(eventRepository, never()).deleteById(anyLong());
        }
    }
}