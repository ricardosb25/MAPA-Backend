package com.mapa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "motors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Motor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "manufacturer", nullable = false, length = 50)
    private String manufacturer;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "displacement_liters", nullable = false, precision = 3, scale = 1)
    private BigDecimal displacementLiters;

    @Column(name = "displacement_cc", nullable = false)
    private Integer displacementCc;

    @Column(name = "compression_ratio", nullable = false, precision = 4, scale = 1)
    private BigDecimal compressionRatio;

    @Column(name = "rpm_cutoff", nullable = false)
    private Integer rpmCutoff;

    @Column(name = "aspiration_type", nullable = false, length = 20)
    private String aspirationType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
