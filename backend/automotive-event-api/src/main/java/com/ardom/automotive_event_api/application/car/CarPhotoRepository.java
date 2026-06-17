package com.ardom.automotive_event_api.application.car;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CarPhotoRepository extends JpaRepository<CarPhoto, Long> {
}
