package com.fleetScan.taxiService.repository.autopark.car;

import com.fleetScan.taxiService.domain.autopark.car.CarPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarPhotoRepository extends JpaRepository <CarPhoto, Long> {
    long countByDriverId(Long driverId);
    List<CarPhoto> findAllByDriverIdOrderByCreatedAtDesc(Long driverId);
    Optional<CarPhoto> findByIdAndDriverId(Long id, Long driverId);

}
