package com.ardom.automotive_event_api.application;

import com.ardom.automotive_event_api.application.car.*;
import com.ardom.automotive_event_api.application.dto.CarDto;
import com.ardom.automotive_event_api.application.dto.request.CreateApplicationRequest;
import com.ardom.automotive_event_api.application.dto.request.RejectApplicationRequest;
import com.ardom.automotive_event_api.application.dto.request.UpdateApplicationRequest;
import com.ardom.automotive_event_api.application.dto.response.*;
import com.ardom.automotive_event_api.application.exception.*;
import com.ardom.automotive_event_api.event.Event;
import com.ardom.automotive_event_api.event.EventRepository;
import com.ardom.automotive_event_api.event.EventStatus;
import com.ardom.automotive_event_api.event.exception.EventNotFoundException;
import com.ardom.automotive_event_api.user.User;
import com.ardom.automotive_event_api.user.exception.UserNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CarRepository carRepository;
    private final CarPhotoRepository carPhotoRepository;
    private final EventRepository eventRepository;
    private final ApplicationMapper applicationMapper;
    private final CarMapper carMapper;

    // -------------------------------------------------------------------------
    // Public
    // -------------------------------------------------------------------------

    @Transactional
    public ApplicationResponse createApplication(Authentication authentication,
                                                 CreateApplicationRequest request) {

        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new EventNotFoundException("Event with id " + request.eventId() + " is not found"));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new EventNotFoundException("Event with id " + request.eventId() + " is not found");
        }

        User user = (User) authentication.getPrincipal();

        if (applicationRepository.existsByUserIdAndEventId(user.getId(), event.getId())) {
            throw new UserAlreadyHasApplicationException(
                    "An application for event " + event.getId() + " already exists");
        }

        Application application = applicationMapper.toBlankApplication(user, event);

        application = applicationRepository.save(application);

        return applicationMapper.toResponse(application, List.of());
    }

    @Transactional
    public ApplicationResponse updateApplication(Authentication authentication, Long id, UpdateApplicationRequest request) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException("Application with id " + id + " is not found"));

        User user = (User) authentication.getPrincipal();
        if (!application.getUser().getId().equals(user.getId())) {
            throw new ApplicationNotFoundException("Application with id " + id + " is not found");
        }

        if (application.getStatus() != ApplicationStatus.DRAFT
                && application.getStatus() != ApplicationStatus.PENDING
                && application.getStatus() != ApplicationStatus.REJECTED) {
            throw new ApplicationNotEditableException(
                    "Application with status " + application.getStatus() + " cannot be edited");
        }

        int existingCarCount = carRepository.countByApplicationId(id);
        List<CarDto> carDtos = request.cars();

        if (existingCarCount + carDtos.size() > 5) {
            throw new TooManyCarsException(
                    "Application already has " + existingCarCount + " car(s); cannot add "
                            + carDtos.size() + " more (max 5 total)");
        }

        List<String> photoKeys = request.cars().stream()
                .map(CarDto::photoKeys)
                .flatMap(List::stream)
                .toList();

        photoKeys.forEach(key -> {
            if (!isUserOwnPhoto(user.getId(), key)) {
                throw new InvalidPhotoKeyException("Invalid photo key: " + key);
            }
        });

        List<Car> savedCars = carRepository.saveAll(
                carDtos.stream()
                        .map(carDto -> carMapper.toEntity(carDto, application))
                        .toList()
        );

        List<CarPhoto> newPhotos = IntStream.range(0, savedCars.size())
                .boxed()
                .flatMap(i -> carDtos.get(i).photoKeys().stream()
                        .map(key -> carMapper.toEntity(key, savedCars.get(i))))
                .toList();
        carPhotoRepository.saveAll(newPhotos);

        int totalCars = existingCarCount + savedCars.size();
        BigDecimal fee = application.getEvent().getApplicationFee()
                .multiply(BigDecimal.valueOf(totalCars));
        application.setFee(fee);

        applicationRepository.save(application);

        return getApplication(authentication, id);
    }

    @Transactional
    public ApplicationResponse submitApplication(Authentication authentication, Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException("Application with id " + id + " is not found"));

        User user = (User) authentication.getPrincipal();
        if (!application.getUser().getId().equals(user.getId())) {
            throw new ApplicationNotFoundException("Application with id " + id + " is not found");
        }

        if (application.getStatus() != ApplicationStatus.DRAFT
                && application.getStatus() != ApplicationStatus.REJECTED) {
            throw new ApplicationNotEditableException(
                    "Application with status " + application.getStatus() + " cannot be submitted");
        }

        int carCount = carRepository.countByApplicationId(id);
        if (carCount == 0) {
            throw new ApplicationNotSubmittableException("Cannot submit an application with no cars");
        }

        if (!application.isSubmittable()) {
            throw new ApplicationNotSubmittableException("The application can no longer be submitted.");
        }

        application.setStatus(ApplicationStatus.PENDING);
        application.setRejectionReason(null);
        applicationRepository.save(application);

        return getApplication(authentication, id);
    }

    public Page<ApplicationSummaryResponse> getUserApplications(Authentication authentication, Pageable pageable) {
        User user = (User) authentication.getPrincipal();
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }

        return getApplications(pageable, user);
    }

    public ApplicationResponse getApplication(Authentication authentication, Long id) {
        Application application = getApplicationById(id);

        User user = (User) authentication.getPrincipal();
        if (!application.getUser().getId().equals(user.getId())) {
            throw new ApplicationNotFoundException("Application with id " + id + " is not found");
        }

        List<Car> cars = carRepository.findAllByApplicationId(id);
        List<Long> carIds = cars.stream()
                .map(Car::getId)
                .toList();

        List<CarPhoto> photos = carPhotoRepository.findAllByCarIdIn(carIds);
        Map<Long, List<CarPhotoResponse>> photosById = photos.stream()
                .collect(Collectors.groupingBy(p -> p.getCar().getId(),
                        Collectors.mapping(carMapper::toResponse, Collectors.toList())
                ));

        return applicationMapper.toResponse(
                application,
                cars.stream()
                        .map(car -> carMapper.toResponse(
                                car, photosById.getOrDefault(car.getId(), List.of())))
                        .collect(Collectors.toList()));
    }

    @Transactional
    public void deleteApplication(Authentication authentication, Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException("Application with id " + id + " is not found"));

        User user = (User) authentication.getPrincipal();
        if (!application.getUser().getId().equals(user.getId())) {
            throw new ApplicationNotFoundException("Application with id " + id + " is not found");
        }

        applicationRepository.deleteById(id);
    }

    // -------------------------------------------------------------------------
    // Admin
    // -------------------------------------------------------------------------

    public Page<ApplicationSummaryResponse> getAllApplications(Pageable pageable) {
        return getApplications(pageable, null);
    }

    public AdminApplicationResponse getApplicationForAdmin(Long id) {
        Application application = getApplicationById(id);

        List<Car> cars = carRepository.findAllByApplicationId(id);
        List<Long> carIds = cars.stream()
                .map(Car::getId)
                .toList();

        List<CarPhoto> photos = carPhotoRepository.findAllByCarIdIn(carIds);
        Map<Long, List<CarPhotoResponse>> photosById = photos.stream()
                .collect(Collectors.groupingBy(p -> p.getCar().getId(),
                        Collectors.mapping(carMapper::toResponse, Collectors.toList())
                ));

        return applicationMapper.toAdminResponse(
                application,
                cars.stream()
                        .map(car -> carMapper.toResponse(
                                car, photosById.getOrDefault(car.getId(), List.of())))
                        .collect(Collectors.toList()));
    }

    @Transactional
    public AdminApplicationResponse updateApplicationStatus(Long id, ApplicationStatus status) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException("Application with id " + id + " is not found"));

        application.setStatus(status);

        return getApplicationForAdmin(id);
    }

    @Transactional
    public AdminApplicationResponse approveApplication(Long id) {
        Application application = getApplicationById(id);

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new InvalidApplicationStatusTransitionException("Application can't be approved");
        }

        application.setStatus(ApplicationStatus.APPROVED_WAITING_PAYMENT);

        return getApplicationForAdmin(id);
    }

    @Transactional
    public AdminApplicationResponse rejectApplication(Long id, RejectApplicationRequest request) {
        Application application = getApplicationById(id);

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new InvalidApplicationStatusTransitionException("Application can't be rejected");
        }

        application.setStatus(ApplicationStatus.REJECTED);
        if (!request.rejectionReason().isBlank()) {
            application.setRejectionReason(request.rejectionReason());
        }

        return getApplicationForAdmin(id);
    }

    // -------------------------------------------------------------------------

    private Page<ApplicationSummaryResponse> getApplications(Pageable pageable, User user) {
        Page<Application> applications;

        if (user != null) {
            applications = applicationRepository.findByUserId(user.getId(), pageable);
        } else {
            applications = applicationRepository.findAll(pageable);
        }

        List<Long> applicationIds = applications.getContent().stream()
                .map(Application::getId)
                .toList();

        List<Car> cars = carRepository.findAllByApplicationIdIn(applicationIds);
        List<Long> carIds = cars.stream()
                .map(Car::getId)
                .toList();

        List<CarPhoto> allPhotos = carPhotoRepository.findAllByCarIdIn(carIds);

        Map<Long, CarPhotoResponse> photoByCarId = allPhotos.stream()
                .collect(Collectors.toMap(
                        photo -> photo.getCar().getId(),
                        carMapper::toResponse,
                        (existing, replacement) -> existing
                ));

        Map<Long, List<CarSummaryResponse>> carResponsesByAppId = cars.stream()
                .collect(Collectors.groupingBy(
                        car -> car.getApplication().getId(),
                        Collectors.mapping(
                                car -> carMapper.toSummaryResponse(car, photoByCarId.getOrDefault(car.getId(), null)),
                                Collectors.toList())
                ));


        return applications.map(app -> applicationMapper.toSummaryResponse(
                app,
                carResponsesByAppId.getOrDefault(app.getId(), List.of())
        ));
    }

    private Application getApplicationById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException("Application with id " + id + " is not found"));
    }

    private boolean isUserOwnPhoto(Long userId, String key) {
        Long extractedUserId = Long.valueOf(key.split("/")[1]);
        return userId.equals(extractedUserId);
    }
}
