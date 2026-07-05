package com.ardom.automotive_event_api.application;

import com.ardom.automotive_event_api.application.car.*;
import com.ardom.automotive_event_api.application.dto.CarDto;
import com.ardom.automotive_event_api.application.dto.request.CreateApplicationRequest;
import com.ardom.automotive_event_api.application.dto.request.RejectApplicationRequest;
import com.ardom.automotive_event_api.application.dto.request.UpdateApplicationRequest;
import com.ardom.automotive_event_api.application.dto.response.AdminApplicationResponse;
import com.ardom.automotive_event_api.application.dto.response.ApplicationResponse;
import com.ardom.automotive_event_api.application.exception.*;
import com.ardom.automotive_event_api.common.email.EmailService;
import com.ardom.automotive_event_api.event.Event;
import com.ardom.automotive_event_api.event.EventRepository;
import com.ardom.automotive_event_api.event.EventStatus;
import com.ardom.automotive_event_api.event.exception.EventNotFoundException;
import com.ardom.automotive_event_api.storage.FileStorageService;
import com.ardom.automotive_event_api.storage.dto.response.PresignedDownloadResponse;
import com.ardom.automotive_event_api.user.Role;
import com.ardom.automotive_event_api.user.User;
import com.ardom.automotive_event_api.user.UserMapper;
import com.ardom.automotive_event_api.user.dto.response.UserResponse;
import com.ardom.automotive_event_api.user.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private CarRepository carRepository;
    @Mock
    private CarPhotoRepository carPhotoRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private Authentication authentication;
    @Mock
    private EmailService emailService;

    private final UserMapper userMapper = new UserMapper();
    private final ApplicationMapper applicationMapper = new ApplicationMapper(userMapper);
    private CarMapper carMapper;

    private ApplicationService applicationService;

    private User user;
    private Event event;

    @BeforeEach
    void setUp() {
        carMapper = new CarMapper(fileStorageService);

        applicationService = new ApplicationService(
                applicationRepository,
                carRepository,
                carPhotoRepository,
                eventRepository,
                applicationMapper,
                carMapper,
                emailService
        );

        user = User.builder().id(1L).name("John").surname("Doe").email("john@example.com").role(Role.USER).build();
        event = Event.builder().id(10L).name("Spring Show").status(EventStatus.PUBLISHED).applicationFee(BigDecimal.valueOf(50)).dateStart(LocalDateTime.now().plusMonths(1)).build();
    }

    // -------------------------------------------------------------------------
    // Method createApplication()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("createApplication()")
    class CreateApplication {

        @Test
        @DisplayName("Should return draft application when event exists")
        void createApplication_shouldReturnDraftApplication_whenEventExists() {
            // given
            CreateApplicationRequest request = new CreateApplicationRequest(event.getId());
            when(authentication.getPrincipal()).thenReturn(user);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> {
                Application app = invocation.getArgument(0);
                app.setId(100L);
                return app;
            });

            // when
            ApplicationResponse response = applicationService.createApplication(authentication, request);

            // then
            assertThat(response.id()).isEqualTo(100L);
            assertThat(response.status()).isEqualTo(ApplicationStatus.DRAFT);
            assertThat(response.eventName()).isEqualTo("Spring Show");
            assertThat(response.cars()).isEmpty();
            verify(applicationRepository).save(any(Application.class));
        }

        @Test
        @DisplayName("Should throw exception when event is not found")
        void createApplication_shouldThrow_whenEventNotFound() {
            // given
            CreateApplicationRequest request = new CreateApplicationRequest(999L);
            when(eventRepository.findById(999L)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> applicationService.createApplication(authentication, request))
                    .isInstanceOf(EventNotFoundException.class);
            verify(applicationRepository, never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // Method updateApplication()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("updateApplication()")
    class UpdateApplication {

        private final Long appId = 5L;

        private CarDto carDto() {
            return new CarDto("BMW", "M3", (short) 2020, "story", List.of(
                    "photos/" + user.getId() + "/uuid1.webp",
                    "photos/" + user.getId() + "/uuid2.webp"));
        }

        @Test
        @DisplayName("Should append cars when draft and owner matches")
        void updateApplication_shouldAppendCars_whenDraftAndOwnerMatches() {
            // given
            Application application = Application.builder()
                    .id(appId).user(user).event(event).status(ApplicationStatus.DRAFT).build();
            UpdateApplicationRequest request = new UpdateApplicationRequest(List.of(carDto()));

            when(authentication.getPrincipal()).thenReturn(user);
            when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));
            when(carRepository.countByApplicationId(appId)).thenReturn(0);

            Car savedCar = Car.builder().id(50L).brand("BMW").model("M3").year((short) 2020)
                    .story("story").application(application).build();
            when(fileStorageService.generateDownloadUrl("photos/" + user.getId() + "/random.webp"))
                    .thenReturn(new PresignedDownloadResponse("https://fake-url", Instant.now().plusSeconds(300)));
            when(carRepository.saveAll(anyList())).thenReturn(List.of(savedCar));

            CarPhoto savedPhoto = CarPhoto.builder().id(200L).s3Key("photos/" + user.getId() + "/random.webp").car(savedCar).build();
            when(carPhotoRepository.saveAll(anyList())).thenReturn(List.of(savedPhoto));

            when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));
            when(carRepository.findAllByApplicationId(appId)).thenReturn(List.of(savedCar));
            when(carPhotoRepository.findAllByCarIdIn(List.of(50L))).thenReturn(List.of(savedPhoto));

            // when
            ApplicationResponse response = applicationService.updateApplication(authentication, appId, request);

            // then
            assertThat(application.getRejectionReason()).isNull();
            assertThat(application.getFee()).isEqualByComparingTo(BigDecimal.valueOf(50));
            assertThat(response.cars()).hasSize(1);
            verify(applicationRepository).save(application);
        }

        @Test
        @DisplayName("Should throw when application not found")
        void updateApplication_shouldThrow_whenApplicationNotFound() {
            // given
            when(applicationRepository.findById(appId)).thenReturn(Optional.empty());
            UpdateApplicationRequest request = new UpdateApplicationRequest(List.of(carDto()));

            // when / then
            assertThatThrownBy(() -> applicationService.updateApplication(authentication, appId, request))
                    .isInstanceOf(ApplicationNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw not found when caller is not owner")
        void updateApplication_shouldThrowNotFound_whenCallerIsNotOwner() {
            // given
            User otherUser = User.builder().id(999L).build();
            Application application = Application.builder()
                    .id(appId).user(otherUser).event(event).status(ApplicationStatus.DRAFT).build();
            UpdateApplicationRequest request = new UpdateApplicationRequest(List.of(carDto()));

            when(authentication.getPrincipal()).thenReturn(user);
            when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));

            // when / then
            assertThatThrownBy(() -> applicationService.updateApplication(authentication, appId, request))
                    .isInstanceOf(ApplicationNotFoundException.class);
            verify(carRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw when application is not editable")
        void updateApplication_shouldThrow_whenApplicationStatusNotEditable() {
            // given
            Application application = Application.builder()
                    .id(appId).user(user).event(event).status(ApplicationStatus.COMPLETED).build();
            UpdateApplicationRequest request = new UpdateApplicationRequest(List.of(carDto()));

            when(authentication.getPrincipal()).thenReturn(user);
            when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));

            // when / then
            assertThatThrownBy(() -> applicationService.updateApplication(authentication, appId, request))
                    .isInstanceOf(ApplicationNotEditableException.class);
            verify(carRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw when total cars exceed five")
        void updateApplication_shouldThrow_whenTotalCarsExceedFive() {
            // given
            Application application = Application.builder()
                    .id(appId).user(user).event(event).status(ApplicationStatus.DRAFT).build();
            UpdateApplicationRequest request = new UpdateApplicationRequest(
                    List.of(carDto(), carDto(), carDto()));

            when(authentication.getPrincipal()).thenReturn(user);
            when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));
            when(carRepository.countByApplicationId(appId)).thenReturn(3);

            // when / then
            assertThatThrownBy(() -> applicationService.updateApplication(authentication, appId, request))
                    .isInstanceOf(TooManyCarsException.class);
            verify(carRepository, never()).saveAll(anyList());
        }
    }

    // -------------------------------------------------------------------------
    // Method submitApplication()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("submitApplication()")
    class SubmitApplication {

        @Test
        @DisplayName("Should throw ApplicationNotFoundException when application is not found")
        void submitApplication_shouldThrowApplicationNotFoundException_whenApplicationNotFound() {
            // when / then
            assertThatThrownBy(() -> applicationService.submitApplication(authentication, 999L))
                    .isInstanceOf(ApplicationNotFoundException.class);
            verify(applicationRepository, never()).save(any(Application.class));
        }

        @Test
        @DisplayName("Should throw ApplicationNotFoundException when user differs from application owner")
        void submitApplication_shouldThrowApplicationNotFoundException_whenUserIsNotOwner() {
            // given
            Application application = getApplication();
            User fakeUser = User.builder()
                    .id(999L)
                    .email("fake@gmail.com")
                    .name("fake")
                    .surname("fake")
                    .role(Role.USER)
                    .build();
            when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
            when(authentication.getPrincipal()).thenReturn(fakeUser);

            // when / then
            assertThatThrownBy(() -> applicationService.submitApplication(authentication, application.getId()))
                    .isInstanceOf(ApplicationNotFoundException.class);
            verify(applicationRepository, never()).save(any(Application.class));
        }

        @Test
        @DisplayName("Should throw ApplicationNotSubmittableException when application has not valid status")
        void submitApplication_shouldThrowApplicationNotSubmittableException_whenStatusIsNotDraftRejected() {
            // given
            Application application = getApplication();
            application.setStatus(ApplicationStatus.PENDING);
            when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
            when(authentication.getPrincipal()).thenReturn(user);

            // when / then
            assertThatThrownBy(() -> applicationService.submitApplication(authentication, application.getId()))
                    .isInstanceOf(ApplicationNotSubmittableException.class);
            verify(applicationRepository, never()).save(any(Application.class));
        }

        @Test
        @DisplayName("Should throw ApplicationNotSubmittableException when application has no cars")
        void submitApplication_shouldThrowApplicationNotSubmittableException_whenApplicationHasNoCars() {
            // given
            Application application = getApplication();
            when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
            when(authentication.getPrincipal()).thenReturn(user);
            when(carRepository.countByApplicationId(application.getId())).thenReturn(0);

            // when / then
            assertThatThrownBy(() -> applicationService.submitApplication(authentication, application.getId()))
                    .isInstanceOf(ApplicationNotSubmittableException.class);
            verify(applicationRepository, never()).save(any(Application.class));
        }

        @Test
        @DisplayName("Should throw ApplicationNotSubmittableException when application is not submittable")
        void submitApplication_shouldThrowApplicationNotSubmittableException_whenApplicationIsNotSubmittable() {
            // given
            Application application = getApplication();
            event.setDateStart(LocalDateTime.now().minusDays(2));
            when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
            when(authentication.getPrincipal()).thenReturn(user);
            when(carRepository.countByApplicationId(application.getId())).thenReturn(1);

            // when / then
            assertThatThrownBy(() -> applicationService.submitApplication(authentication, application.getId()))
                    .isInstanceOf(ApplicationNotSubmittableException.class);
            verify(applicationRepository, never()).save(any(Application.class));
        }

        @Test
        @DisplayName("Should save application and return response")
        void submitApplication_shouldSaveApplicationAndReturnResponse_whenApplicationValid() {
            // given
            Application application = getApplication();
            when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
            when(authentication.getPrincipal()).thenReturn(user);
            when(carRepository.countByApplicationId(application.getId())).thenReturn(1);

            // when
            ApplicationResponse response = applicationService.submitApplication(authentication, application.getId());

            // then
            ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
            verify(applicationRepository).save(captor.capture());

            Application saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.PENDING);
            assertThat(saved.getRejectionReason()).isNull();
            assertThat(response.id()).isEqualTo(application.getId());
            assertThat(response.eventName()).isEqualTo(application.getEvent().getName());
        }
    }

    // -------------------------------------------------------------------------
    // Method getUserApplications()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getUserApplications()")
    class GetUserApplications {

        @Test
        @DisplayName("Should return page of summaries when user is authenticated")
        void getUserApplications_shouldReturnPageOfSummaries_whenUserAuthenticated() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Application application = Application.builder()
                    .id(1L).user(user).event(event).status(ApplicationStatus.PENDING).build();

            when(authentication.getPrincipal()).thenReturn(user);
            when(applicationRepository.findByUserId(user.getId(), pageable))
                    .thenReturn(new PageImpl<>(List.of(application)));
            when(carRepository.findAllByApplicationIdIn(List.of(1L))).thenReturn(List.of());
            when(carPhotoRepository.findAllByCarIdIn(anyList())).thenReturn(List.of());

            // when
            var result = applicationService.getUserApplications(authentication, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().id()).isEqualTo(1L);
            assertThat(result.getContent().getFirst().status())
                    .isEqualTo(ApplicationStatus.PENDING);
            assertThat(result.getContent().getFirst().eventName())
                    .isEqualTo("Spring Show");
        }

        @Test
        @DisplayName("Should throw when principal is null")
        void getUserApplications_shouldThrow_whenPrincipalIsNull() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            when(authentication.getPrincipal()).thenReturn(null);

            // when / then
            assertThatThrownBy(() -> applicationService.getUserApplications(authentication, pageable))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Method getApplication()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getApplication()")
    class GetApplication {

        @Test
        @DisplayName("Should return response with cars and photos when application exists")
        void getApplication_shouldReturnResponseWithCarsAndPhotos_whenApplicationExists() {
            // given
            Application application = Application.builder()
                    .id(1L).user(user).event(event).status(ApplicationStatus.PENDING)
                    .fee(BigDecimal.valueOf(50)).build();
            Car car = Car.builder().id(20L).brand("BMW").model("M3").year((short) 2021)
                    .application(application).build();
            CarPhoto photo = CarPhoto.builder().id(30L).s3Key("photos/" + user.getId() + "/random.webp").car(car).build();

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(authentication.getPrincipal()).thenReturn(user);
            when(carRepository.findAllByApplicationId(1L)).thenReturn(List.of(car));
            when(carPhotoRepository.findAllByCarIdIn(List.of(20L))).thenReturn(List.of(photo));
            when(fileStorageService.generateDownloadUrl("photos/" + user.getId() + "/random.webp"))
                    .thenReturn(new PresignedDownloadResponse("https://fake-url", Instant.now().plusSeconds(300)));

            // when
            ApplicationResponse response = applicationService.getApplication(authentication, 1L);

            // then
            assertThat(response.cars()).hasSize(1);
            assertThat(response.cars().getFirst().carPhotos()).hasSize(1);
            assertThat(response.cars().getFirst().carPhotos().getFirst().url()).isEqualTo("https://fake-url");
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.status()).isEqualTo(ApplicationStatus.PENDING);
            assertThat(response.fee()).isEqualByComparingTo("50");
            assertThat(response.cars().getFirst().brand()).isEqualTo("BMW");
            assertThat(response.cars().getFirst().model()).isEqualTo("M3");
        }

        @Test
        @DisplayName("Should return empty car photos when car has no photos")
        void getApplication_shouldReturnEmptyCarPhotos_whenCarHasNoPhotos() {
            // given
            Application application = Application.builder()
                    .id(1L).user(user).event(event).status(ApplicationStatus.DRAFT).build();
            Car car = Car.builder().id(20L).brand("BMW").model("M3").year((short) 2021)
                    .application(application).build();

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(authentication.getPrincipal()).thenReturn(user);
            when(carRepository.findAllByApplicationId(1L)).thenReturn(List.of(car));
            when(carPhotoRepository.findAllByCarIdIn(List.of(20L))).thenReturn(List.of());

            // when
            ApplicationResponse response = applicationService.getApplication(authentication, 1L);

            // then
            assertThat(response.cars()).hasSize(1);
            assertThat(response.cars().getFirst().carPhotos()).isEmpty();
        }

        @Test
        @DisplayName("Should throw when application is not found")
        void getApplication_shouldThrow_whenApplicationNotFound() {
            // given
            when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> applicationService.getApplication(authentication, 1L))
                    .isInstanceOf(ApplicationNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Method deleteApplication()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("deleteApplication()")
    class DeleteApplication {

        @Test
        @DisplayName("Should delete when owner matches")
        void deleteApplication_shouldDelete_whenOwnerMatches() {
            // given
            Application application = Application.builder()
                    .id(1L).user(user).event(event).status(ApplicationStatus.DRAFT).build();
            when(authentication.getPrincipal()).thenReturn(user);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

            // when
            applicationService.deleteApplication(authentication, 1L);

            // then
            verify(applicationRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw not found when caller is not owner")
        void deleteApplication_shouldThrowNotFound_whenCallerIsNotOwner() {
            // given
            User otherUser = User.builder().id(999L).build();
            Application application = Application.builder()
                    .id(1L).user(otherUser).event(event).status(ApplicationStatus.DRAFT).build();
            when(authentication.getPrincipal()).thenReturn(user);
            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

            // when / then
            assertThatThrownBy(() -> applicationService.deleteApplication(authentication, 1L))
                    .isInstanceOf(ApplicationNotFoundException.class);
            verify(applicationRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw when application is not found")
        void deleteApplication_shouldThrow_whenApplicationNotFound() {
            // given
            when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> applicationService.deleteApplication(authentication, 1L))
                    .isInstanceOf(ApplicationNotFoundException.class);
            verify(applicationRepository, never()).deleteById(any());
        }
    }

    // -------------------------------------------------------------------------
    // Method getAllApplications()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getAllApplications()")
    class GetAllApplications {

        @Test
        @DisplayName("Should return page of summaries")
        void getAllApplications_shouldReturnPageOfSummaries() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Application application = Application.builder()
                    .id(1L).user(user).event(event).status(ApplicationStatus.PENDING).build();

            when(applicationRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(application)));
            when(carRepository.findAllByApplicationIdIn(List.of(1L))).thenReturn(List.of());
            when(carPhotoRepository.findAllByCarIdIn(anyList())).thenReturn(List.of());

            // when
            var result = applicationService.getAllApplications(pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(applicationRepository, never()).findByUserId(anyLong(), any());
        }
    }

    // -------------------------------------------------------------------------
    // Method getApplicationForAdmin()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getApplicationForAdmin()")
    class GetApplicationForAdmin {

        @Test
        @DisplayName("Should return response with user details when application exists")
        void getApplicationForAdmin_shouldReturnResponseWithUserDetails_whenApplicationExists() {
            // given
            Application application = Application.builder()
                    .id(1L).user(user).event(event).status(ApplicationStatus.PENDING).build();
            UserResponse userResponse = new UserResponse(user.getId(), "John", "Doe", "john@example.com", Role.USER);

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(carRepository.findAllByApplicationId(1L)).thenReturn(List.of());
            when(carPhotoRepository.findAllByCarIdIn(anyList())).thenReturn(List.of());

            // when
            AdminApplicationResponse response = applicationService.getApplicationForAdmin(1L);

            // then
            assertThat(response.user()).isEqualTo(userResponse);
            assertThat(response.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw when application not found")
        void getApplicationForAdmin_shouldThrow_whenApplicationNotFound() {
            // given
            when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> applicationService.getApplicationForAdmin(1L))
                    .isInstanceOf(ApplicationNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Method updateApplicationStatus()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("updateApplicationStatus()")
    class UpdateApplicationStatus {

        @Test
        @DisplayName("Should update status when application exists")
        void updateApplicationStatus_shouldUpdateStatus_whenApplicationExists() {
            // given
            Application application = Application.builder()
                    .id(1L).user(user).event(event).status(ApplicationStatus.PENDING).build();

            when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
            when(carRepository.findAllByApplicationId(1L)).thenReturn(List.of());
            when(carPhotoRepository.findAllByCarIdIn(anyList())).thenReturn(List.of());

            // when
            AdminApplicationResponse response =
                    applicationService.updateApplicationStatus(1L, ApplicationStatus.APPROVED_WAITING_PAYMENT);

            // then
            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED_WAITING_PAYMENT);
            assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED_WAITING_PAYMENT);
        }

        @Test
        @DisplayName("Should throw when application is not found")
        void updateApplicationStatus_shouldThrow_whenApplicationNotFound() {
            // given
            when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() ->
                    applicationService.updateApplicationStatus(1L, ApplicationStatus.REJECTED))
                    .isInstanceOf(ApplicationNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Method approveApplication()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("approveApplication()")
    class ApproveApplication {

        @Test
        @DisplayName("Should throw InvalidApplicationStatusTransitionException when application not in pending status")
        void approveApplication_shouldThrowInvalidApplicationStatusTransitionException_whenApplicationNotPENDING() {
            // given
            Application application = getApplication();
            when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

            // when / then
            assertThatThrownBy(() -> applicationService.approveApplication(application.getId()))
                    .isInstanceOf(InvalidApplicationStatusTransitionException.class);
        }

        @Test
        @DisplayName("Should change application status to APPROVED_WAITING_PAYMENT")
        void approveApplication_shouldChangeApplicationStatusToAPPROVED_WAITING_PAYMENT() {
            // given
            Application application = getApplication();
            application.setStatus(ApplicationStatus.PENDING);
            when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

            // when
            AdminApplicationResponse response = applicationService.approveApplication(application.getId());

            // then
            assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED_WAITING_PAYMENT);
        }
    }

    // -------------------------------------------------------------------------
    // Method rejectApplication()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("rejectApplication()")
    class RejectApplication {

        @Test
        @DisplayName("Should throw InvalidApplicationStatusTransitionException when application not in pending status")
        void rejectApplication_shouldThrowInvalidApplicationStatusTransitionException_whenApplicationNotPENDING() {
            // given
            Application application = getApplication();
            when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

            // when / then
            assertThatThrownBy(() -> applicationService.rejectApplication(application.getId(), new RejectApplicationRequest("")))
                    .isInstanceOf(InvalidApplicationStatusTransitionException.class);
        }

        @Test
        @DisplayName("Should change application status to REJECTED and leave rejected reason null")
        void rejectApplication_shouldChangeApplicationStatusToREJECTED() {
            // given
            Application application = getApplication();
            application.setStatus(ApplicationStatus.PENDING);
            when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

            // when
            AdminApplicationResponse response = applicationService.rejectApplication(application.getId(), new RejectApplicationRequest(""));

            // then
            assertThat(response.status()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(response.rejectionReason()).isNull();
        }

        @Test
        @DisplayName("Should change application status to REJECTED and insert rejection reason")
        void rejectApplication_shouldChangeApplicationStatusToREJECTEDAndInsertRejectionReason() {
            // given
            Application application = getApplication();
            application.setStatus(ApplicationStatus.PENDING);
            when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

            // when
            AdminApplicationResponse response = applicationService.rejectApplication(application.getId(), new RejectApplicationRequest("Rejection reason"));

            // then
            assertThat(response.status()).isEqualTo(ApplicationStatus.REJECTED);
            assertThat(response.rejectionReason()).isEqualTo("Rejection reason");
        }
    }

    private Application getApplication() {
        return Application.builder()
                .id(1L)
                .user(user)
                .event(event)
                .status(ApplicationStatus.DRAFT)
                .build();
    }
}
