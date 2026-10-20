package com.mapa.domain;

import com.mapa.domain.converters.IntegerListConverter;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(
        name = "ecu_map_defaults",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ecu_map_defaults_displacement_map_type",
                columnNames = {"displacement_class", "map_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcuMapDefault {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "displacement_class", nullable = false, precision = 3, scale = 1)
    private BigDecimal displacementClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "map_type", nullable = false, length = 20)
    private EcuMapType mapType;

    @Convert(converter = IntegerListConverter.class)
    @Column(name = "rpm_breakpoints", nullable = false, length = 2000)
    private List<Integer> rpmBreakpoints;

    @Convert(converter = IntegerListConverter.class)
    @Column(name = "load_breakpoints", nullable = false, length = 1000)
    private List<Integer> loadBreakpoints;

    @Convert(converter = IntegerMatrixConverter.class)
    @Column(name = "factory_values", nullable = false, length = 4000)
    private List<List<Integer>> factoryValues;

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