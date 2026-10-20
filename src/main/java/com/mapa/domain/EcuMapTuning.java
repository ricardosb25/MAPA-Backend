package com.mapa.domain;

import com.mapa.domain.converters.IntegerMatrixConverter;
import com.mapa.domain.enums.EcuMapType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(
        name = "ecu_map_tunings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ecu_map_tunings_user_motor_map_type",
                columnNames = {"user_id", "motor_id", "map_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcuMapTuning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "motor_id", nullable = false)
    private Long motorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "map_type", nullable = false, length = 20)
    private EcuMapType mapType;

    @Convert(converter = IntegerMatrixConverter.class)
    @Column(name = "tuned_values", nullable = false, length = 4000)
    private List<List<Integer>> tunedValues;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
