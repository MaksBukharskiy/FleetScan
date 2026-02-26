package com.fleetScan.taxiService.domain.autopark.vehicle;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "black_list")
public class BlackList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "plate_number", nullable = false, unique = true, length = 16)
    private String plateNumber;

    @Column(name = "created_by_chat_id", nullable = false)
    private Long createdByChatId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "reason", nullable = false, length = 512)
    private String reason;

    @OneToMany(mappedBy = "blackList")
    private List<DetectedVehicle> detectedVehicles;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
