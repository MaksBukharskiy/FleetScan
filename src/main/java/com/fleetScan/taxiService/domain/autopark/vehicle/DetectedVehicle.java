package com.fleetScan.taxiService.domain.autopark.vehicle;

import com.fleetScan.taxiService.domain.autopark.vehicle.VehicleStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "detected_vehicle")
public class DetectedVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private double confidence;

    @Column(name = "plate_number", nullable = false, length = 16)
    private String plateNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    @Column(name = "vehicle_condition", nullable = false, length = 32)
    private String vehicleCondition;

    @Column(name = "condition_confidence", nullable = false)
    private double conditionConfidence;

    @Column(name = "decision_reason", nullable = false, length = 512)
    private String decisionReason;

    @Column(name = "source_chat_id", nullable = false)
    private Long sourceChatId;

    @Column(nullable = false)
    private String imagePath;

    @ManyToOne
    @JoinColumn(name = "blacklist_id")
    private BlackList blackList;

    @CreationTimestamp
    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;
}
