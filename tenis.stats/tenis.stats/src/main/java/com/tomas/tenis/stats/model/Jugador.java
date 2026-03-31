package com.tomas.tenis.stats.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
@Entity
  @Table(name = "jugadores")
  public class Jugador {
    // Getters
    @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;
      private String nombre;
      private String nacionalidad;
      private Integer rankingActual;

        // JPA exige un constructor vacío.
        public Jugador() {}

}

