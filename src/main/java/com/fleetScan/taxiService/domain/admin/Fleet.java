package com.fleetScan.taxiService.domain.admin;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "fleet")
@NoArgsConstructor
@AllArgsConstructor
public class Fleet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", unique = true, nullable = false, length = 40)
    private String publicId;

    @Column(name = "admin_chat_id", nullable = false, length = 40)
    private Long adminChatId;

    @Column(name = "name", unique = false, nullable = false, length = 50)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at_time")
    private LocalDateTime createdAtTime;

}
