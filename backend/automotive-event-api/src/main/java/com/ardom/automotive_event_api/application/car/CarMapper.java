package com.ardom.automotive_event_api.application.car;

import com.ardom.automotive_event_api.application.Application;
import com.ardom.automotive_event_api.application.dto.CarDto;
import com.ardom.automotive_event_api.application.dto.response.CarPhotoResponse;
import com.ardom.automotive_event_api.application.dto.response.CarResponse;
import com.ardom.automotive_event_api.application.dto.response.CarSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CarMapper {

    public CarResponse toResponse(Car car, List<CarPhotoResponse> photos) {
        return new CarResponse(
                car.getId(),
                car.getBrand(),
                car.getModel(),
                car.getYear(),
                car.getStory(),
                photos
        );
    }

    public CarSummaryResponse toSummaryResponse(Car car, CarPhotoResponse photo) {
        return new CarSummaryResponse(
                car.getId(),
                car.getBrand(),
                car.getModel(),
                car.getYear(),
                photo
        );
    }

    public CarPhotoResponse toResponse(CarPhoto photo) {
        return new CarPhotoResponse(
                photo.getId(),
                "http://fakes3url" // TODO implement url generation
        );
    }

    public Car toEntity(CarDto dto, Application application) {
        return Car.builder()
                .brand(dto.brand())
                .model(dto.model())
                .year(dto.year())
                .story(dto.story())
                .application(application)
                .build();
    }

    public CarPhoto toEntity(String key, Car car) {
        return CarPhoto.builder()
                .s3Key(key)
                .car(car)
                .build();
    }
}
