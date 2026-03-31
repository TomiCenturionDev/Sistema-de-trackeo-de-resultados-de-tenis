package com.tomas.tenis.stats.util;

import com.tomas.tenis.stats.model.EstadoPartido;
import com.tomas.tenis.stats.model.Resultado;
import com.tomas.tenis.stats.model.ResultadoParcial;
import java.util.ArrayList;
import java.util.List;

public class MarcadorParser {

    public static ProcesamientoMarcador parsear(String scoreRaw) {
        List<Resultado> setsCompletos = new ArrayList<>();
        ResultadoParcial setIncompleto = null;
        String setIncompletoRaw = null;
        EstadoPartido estado = EstadoPartido.EN_CURSO;

        if (scoreRaw == null || scoreRaw.isBlank() || scoreRaw.equals("0-0")) {
            return new ProcesamientoMarcador(setsCompletos, EstadoPartido.PROGRAMADO, null, null);
        }

        String[] segmentos = scoreRaw.trim().split("\\s+");

        for (String seg : segmentos) {
            String limpio = seg.trim().toUpperCase();

            if (limpio.contains("RET") || limpio.contains("RETIRO")) {
                estado = EstadoPartido.RETIRO;
                break;
            }
            if (limpio.contains("DSQ") || limpio.contains("DEF")) {
                estado = EstadoPartido.DESCALIFICACION;
                break;
            }

            if (!limpio.contains("-")) continue;

            try {
                String[] partesJuegos = limpio.split("-");
                if (partesJuegos.length < 2) continue;

                int j1 = Integer.parseInt(partesJuegos[0].replaceAll("[^0-9]", ""));

                int j2;
                int tieBreak = 0;
                String p2Raw = partesJuegos[1];

                if (p2Raw.contains("(")) {
                    String juegosJ2Str = p2Raw.substring(0, p2Raw.indexOf("(")).replaceAll("[^0-9]", "");
                    j2 = Integer.parseInt(juegosJ2Str);

                    String tieStr = p2Raw.substring(p2Raw.indexOf("(") + 1, p2Raw.indexOf(")")).replaceAll("[^0-9]", "");
                    tieBreak = Integer.parseInt(tieStr);
                } else {
                    j2 = Integer.parseInt(p2Raw.replaceAll("[^0-9]", ""));
                }

                // ✅ VALIDACIÓN CORREGIDA CON TIE-BREAK
                boolean setValido = false;

                // Caso normal (ej: 6-3, 6-4, 7-5)
                if ((j1 >= 6 || j2 >= 6) && Math.abs(j1 - j2) >= 2) {
                    setValido = true;
                }

                // Caso tie-break obligatorio (6-7 o 7-6)
                if ((j1 == 7 && j2 == 6) || (j1 == 6 && j2 == 7)) {
                    if (tieBreak > 0) {
                        setValido = true;
                    } else {
                        throw new IllegalArgumentException("Tie-break faltante en set: " + limpio);
                    }
                }

                if (setValido) {
                    setsCompletos.add(new Resultado(j1, j2, tieBreak));
                } else {
                    setIncompleto = new ResultadoParcial(j1, j2);
                    setIncompletoRaw = limpio;
                }

            } catch (Exception e) {
                setIncompletoRaw = limpio;
                System.err.println("Error parseando segmento: " + limpio + " | " + e.getMessage());
            }
        }

        return new ProcesamientoMarcador(setsCompletos, estado, setIncompleto, setIncompletoRaw);
    }
}