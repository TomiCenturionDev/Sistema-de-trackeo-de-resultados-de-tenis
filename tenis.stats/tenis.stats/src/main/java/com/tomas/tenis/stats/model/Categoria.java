package com.tomas.tenis.stats.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "categorias")
@Data
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;       // Grand Slam, Masters 1000
    private Integer puntos;      // 2000, 1000

    @Column(name = "tipo_de_torneo")
    private String tipoDeTorneo; // valor persistido en DB

    /**
     * 🔥 Devuelve el tipo como enum (para lógica de negocio / Strategy)
     */
    @Transient
    public TipoTorneo getTipo() {
        if (tipoDeTorneo == null) {
            throw new IllegalStateException("Tipo de torneo no definido");
        }

        String tipo = tipoDeTorneo.trim().toUpperCase();

        if (tipo.contains("GS")) return TipoTorneo.GRAND_SLAM;
        if (tipo.contains("M1000") || tipo.contains("MASTER")) return TipoTorneo.M1000;
        if (tipo.contains("ATP")) return TipoTorneo.ATP;
        if (tipo.contains("CHALLENGER")) return TipoTorneo.CHALLENGER;

        throw new IllegalArgumentException("Tipo de torneo desconocido: " + tipoDeTorneo);
    }

    /**
     * 🔥 Permite setear el tipo usando enum (clave para tests y lógica)
     */
    public void setTipo(TipoTorneo tipo) {
        if (tipo == null) {
            this.tipoDeTorneo = null;
            return;
        }

        switch (tipo) {
            case GRAND_SLAM:
                this.tipoDeTorneo = "GRAND SLAM";
                break;
            case ATP:
                this.tipoDeTorneo = "ATP";
                break;
            case CHALLENGER:
                this.tipoDeTorneo = "CHALLENGER";
                break;
            default:
                throw new IllegalArgumentException("Tipo no soportado: " + tipo);
        }
    }
}