package com.fleetScan.taxiService.repository.autopark.vehicle;

import com.fleetScan.taxiService.domain.autopark.vehicle.BlackList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlackListRepository extends JpaRepository<BlackList, Long> {
    Optional<BlackList> findByPlateNumberAndIsActiveTrue(String plateNumber);
    Optional<BlackList> findByPlateNumber(String plateNumber);
}
