package com.tomas.tenis.stats.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;

@Setter
@Getter
@Entity
@Table(name = "partidos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_partido_unico", columnNames = {"fecha", "torneo", "fase", "jugador1_id", "jugador2_id"})
})
public class Partido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) // Agregado: Un partido siempre tiene fecha
    private LocalDate fecha;

    @Column(nullable = false)
    private String torneo;

    @Column(nullable = false)
    private String pais;

    @Column(nullable = false) // Ajuste: Coherencia con el DTO
    private String ciudad;

    @ManyToOne
    @JoinColumn(name = "jugador1_id", nullable = false)
    private Jugador jugador1;

    @ManyToOne
    @JoinColumn(name = "jugador2_id", nullable = false)
    private Jugador jugador2;

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Superficie superficie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FaseTorneo fase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPartido estado = EstadoPartido.PROGRAMADO;

    @Column(nullable = false)
    private String resultado = "0-0";

    @Column(nullable = false)
    private String ganador = "TBD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncStatus syncStatus = SyncStatus.PENDING;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column(name = "external_id", unique = true)
    private String externalId;

    private String jugadorRetirado;

    // --- CONSTRUCTORES ---

    public Partido() {
        // Requerido por Hibernate
    }

    public Partido(FaseTorneo fase) {
        this.fase = Objects.requireNonNull(fase, "La fase es obligatoria para crear un partido");
    }
}