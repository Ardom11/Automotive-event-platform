package com.ardom.automotive_event_api.application.car;

import com.ardom.automotive_event_api.application.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findAllByApplicationId(Long id);

    List<Car> findAllByApplicationIdIn(Collection<Long> applications);

    Integer countByApplicationId(Long id);
}
