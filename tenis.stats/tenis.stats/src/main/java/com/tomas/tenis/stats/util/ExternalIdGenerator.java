package com.tomas.tenis.stats.util;

import com.tomas.tenis.stats.model.Partido;
import org.springframework.stereotype.Component;

@Component
public class ExternalIdGenerator {

    public String generarExternalId(Partido partido) {

        int anio = partido.getFecha().getYear();
        String torneoLimpio = limpiar(partido.getTorneo());
        String paisLimpio = limpiar(partido.getPais());
        String fase = partido.getFase().name();
        int puntos = (partido.getCategoria() != null)
                ? partido.getCategoria().getPuntos()
                : 0;

        String externalId;

        if (fase.equals("F")) {
            externalId = torneoLimpio + "_" + paisLimpio + "_" + puntos + "_" + anio + "_" + fase;
        } else {
            String j1 = limpiar(partido.getJugador1().getNombre());
            String j2 = limpiar(partido.getJugador2().getNombre());

            externalId = torneoLimpio + "_" + paisLimpio + "_" + puntos + "_" + anio + "_" + fase
                    + "_" + j1 + "_VS_" + j2;
        }

        return externalId;
    }

    private String limpiar(String valor) {
        return valor.replaceAll("\\s+", "").toUpperCase();
    }
}