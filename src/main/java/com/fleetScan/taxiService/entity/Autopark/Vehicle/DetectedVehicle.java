package com.fleetScan.taxiService.entity.Autopark.Vehicle;

import com.fleetScan.taxiService.entity.Autopark.Vehicle.Status.VehicleStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class DetectedVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    @Column(nullable = false, unique = true)
    private String imagePath;

    @ManyToOne
    @JoinColumn(name = "blacklist_id")
    private BlackList blackList;
}
