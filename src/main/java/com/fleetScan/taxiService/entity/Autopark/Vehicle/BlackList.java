package com.fleetScan.taxiService.entity.Autopark.Vehicle;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class BlackList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String reason;

    @OneToMany(mappedBy = "blackList")
    private List<DetectedVehicle> detectedVehicles;

}
