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
    private String tipoDeTorneo; // GS, ATP, Challenger
}