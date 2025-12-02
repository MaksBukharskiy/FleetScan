package com.fleetScan.taxiService.repository.Autopark;

import com.fleetScan.taxiService.entity.Autopark.Driver.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByName(String driverName);
    Optional<Driver> findByChatId(Long id);
    Optional<Driver> findByInviteCode(String inviteCode);

}
