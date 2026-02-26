package com.fleetScan.taxiService.repository.autopark;

import com.fleetScan.taxiService.domain.autopark.driver.Driver;
import com.fleetScan.taxiService.domain.autopark.driver.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByName(String driverName);
    Optional<Driver> findByChatId(Long id);
    Optional<Driver> findByChatIdAndIsActiveTrue(Long id);
    Optional<Driver> findByInviteCode(String inviteCode);
    List<Driver> findAllByFleetId(Long fleetId);
    List<Driver> findAllByFleetIdAndRoleInAndIsActiveTrue(Long fleetId, List<UserRole> roles);
    Optional<Driver> findByFleetIdAndName(Long fleetId, String name);

}
