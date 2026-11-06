package com.mapa.domain;

import com.mapa.domain.enums.EngineStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "dynamometer_tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DynamometerTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "motor_id", nullable = false)
    private Long motorId;

    @Column(name = "max_power", nullable = false)
    private Double maxPower;

    @Column(name = "max_torque", nullable = false)
    private Double maxTorque;

    @Column(name = "average_afr", nullable = false)
    private Double averageAfr;

    @Enumerated(EnumType.STRING)
    @Column(name = "engine_status", nullable = false, length = 30)
    private EngineStatus engineStatus;

    @Column(name = "tested_at", nullable = false, updatable = false)
    private OffsetDateTime testedAt;

    @PrePersist
    protected void onCreate() {
        if (testedAt == null) {
            testedAt = OffsetDateTime.now();
        }
    }
}
