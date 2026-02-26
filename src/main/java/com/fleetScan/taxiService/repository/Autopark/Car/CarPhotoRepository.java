package com.fleetScan.taxiService.repository.autopark.car;

import com.fleetScan.taxiService.domain.autopark.car.CarPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarPhotoRepository extends JpaRepository <CarPhoto, Long> {

}
