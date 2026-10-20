package com.mapa.domain;

import com.mapa.domain.enums.EngineStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "desafio_progresso",
        uniqueConstraints = @UniqueConstraint(name = "uk_desafio_aluno", columnNames = {"desafio_id", "aluno_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "desafio_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private User student;

    @Column(name = "selecionado", nullable = false)
    @Builder.Default
    private boolean selected = false;

    @Column(name = "tentativas", nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "concluido", nullable = false)
    @Builder.Default
    private boolean completed = false;

    @Column(name = "ultima_potencia")
    private Double lastPeakPower;

    @Enumerated(EnumType.STRING)
    @Column(name = "ultimo_status", length = 30)
    private EngineStatus lastEngineStatus;

    @Column(name = "selecionado_em")
    private OffsetDateTime selectedAt;

    @Column(name = "concluido_em")
    private OffsetDateTime completedAt;
}
