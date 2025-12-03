package com.fleetScan.taxiService.entity.Autopark.Driver;

import com.fleetScan.taxiService.entity.Admin.Fleet;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "driver")
@NoArgsConstructor
@AllArgsConstructor
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "fleet_id")
    private Fleet fleet;

    @Column(nullable = false)
    private String name;

    @Column(name = "chat_id", unique = true)
    private Long chatId;

    @Column(name = "invite_code", unique = true)
    private String inviteCode;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}
