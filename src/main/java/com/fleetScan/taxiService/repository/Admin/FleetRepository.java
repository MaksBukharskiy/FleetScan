package com.fleetScan.taxiService.repository.admin;

import com.fleetScan.taxiService.domain.admin.Fleet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FleetRepository extends JpaRepository<Fleet, Long> {

    Optional<Fleet> findById(Long id);
    Optional<Fleet> findByName(String name);
    Optional<Fleet> findByPublicId(String publicId);
    Optional<Fleet> findByAdminChatId(Long adminChatId);

    boolean existsByAdminChatId(Long adminChatId);
    boolean existsByName(String name);

}
