package com.tomas.tenis.stats.dto;

import com.tomas.tenis.stats.model.*; // Importamos las entidades
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PartidoRequest {

    private Long id; // Agregado para el JSON

    private String fecha; // Agregado para el JSON

    @NotBlank(message = "El nombre del torneo es obligatorio")
    private String torneo;

    @NotNull(message = "La categoría es obligatoria")
    private Categoria categoria; // Cambiado de Long categoriaId a Objeto Categoria

    @NotBlank(message = "El código de país es obligatorio")
    @Size(min = 2, max = 2, message = "El código de país debe ser de 2 letras (ISO)")
    private String pais;

    @NotNull(message = "La superficie es obligatoria (ARCILLA, CESPED, CEMENTO)")
    private Superficie superficie;

    @NotNull(message = "El jugador 1 es obligatorio")
    private Jugador jugador1; // Cambiado de Long jugador1Id a Objeto Jugador

    @NotNull(message = "El jugador 2 es obligatorio")
    private Jugador jugador2; // Cambiado de Long jugador2Id a Objeto Jugador

    private String ciudad;

    private EstadoPartido estado; // Agregado para el JSON

    private String resultado = "0-0";

    private String ganador; // Agregado para el JSON

    @NotNull(message = "Debes especificar la fase (R32, QF, SF, F, etc.)")
    private FaseTorneo fase;
}