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
@Table(name = "motores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Motor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fabricante", nullable = false, length = 50)
    private String manufacturer;

    @Column(name = "nome", nullable = false, length = 50)
    private String name;

    @Column(name = "descricao", length = 255)
    private String description;

    @Column(name = "cilindrada_litros", nullable = false, precision = 3, scale = 1)
    private BigDecimal displacementLiters;

    @Column(name = "cilindrada_cc", nullable = false)
    private Integer displacementCc;

    @Column(name = "taxa_compressao", nullable = false, precision = 4, scale = 1)
    private BigDecimal compressionRatio;

    @Column(name = "corte_rpm", nullable = false)
    private Integer rpmCutoff;

    @Column(name = "tipo_aspiracao", nullable = false, length = 20)
    private String aspirationType;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
