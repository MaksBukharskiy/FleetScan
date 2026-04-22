package com.fleetScan.taxiService.repository.autopark.vehicle;

import com.fleetScan.taxiService.domain.autopark.vehicle.DetectedVehicle;
import com.fleetScan.taxiService.domain.autopark.vehicle.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface DetectedVehicleRepository extends JpaRepository<DetectedVehicle, Long>, JpaSpecificationExecutor<DetectedVehicle> {
    long countByStatus(VehicleStatus status);
    long countBySourceChatId(Long sourceChatId);
    long countByDetectedAtBetween(LocalDateTime from, LocalDateTime to);
    long countByStatusAndDetectedAtBetween(VehicleStatus status, LocalDateTime from, LocalDateTime to);
    long countByPlateNumberAndDetectedAtBetween(String plateNumber, LocalDateTime from, LocalDateTime to);

    List<DetectedVehicle> findAllByDetectedAtBetweenOrderByDetectedAtDesc(LocalDateTime from, LocalDateTime to);
    List<DetectedVehicle> findAllBySourceChatIdOrderByDetectedAtDesc(Long sourceChatId);
    List<DetectedVehicle> findAllBySourceChatIdAndDetectedAtBetweenOrderByDetectedAtDesc(
            Long sourceChatId,
            LocalDateTime from,
            LocalDateTime to
    );
    List<DetectedVehicle> findAllByPlateNumberContainingIgnoreCaseAndDetectedAtBetweenOrderByDetectedAtDesc(
            String plateNumber,
            LocalDateTime from,
            LocalDateTime to
    );
    List<DetectedVehicle> findAllByStatusAndDetectedAtBetweenOrderByDetectedAtDesc(
            VehicleStatus status,
            LocalDateTime from,
            LocalDateTime to
    );
}
