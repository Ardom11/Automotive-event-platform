package com.ardom.automotive_event_api.application.car;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CarPhotoRepository extends JpaRepository<CarPhoto, Long> {
    List<CarPhoto> findAllByCarIdIn(Collection<Long> cars);
}
