package com.tomas.tenis.stats.model;

/**
 * Representa el resultado inmutable de un solo set terminado.
 *
 * @param tieBreak 0 si no hubo tie-break
 */
public record Resultado(int juegosJ1, int juegosJ2, int tieBreak) {

    public Resultado {
        validarMarcador(juegosJ1, juegosJ2, tieBreak);
    }

    @Override
    @SuppressWarnings("unused")
    public int tieBreak() {
        return tieBreak;
    }

    private void validarMarcador(int j1, int j2, int tieBreak) {

        if (j1 < 0 || j2 < 0) {
            throw new IllegalArgumentException("Los juegos no pueden ser negativos: " + j1 + "-" + j2);
        }

        if (j1 == 6 && j2 == 6) {
            throw new IllegalArgumentException("Set no puede terminar 6-6");
        }

        int diff = Math.abs(j1 - j2);

        // ✅ 1. SET NORMAL (solo hasta 7)
        boolean setNormal =
                (j1 <= 7 && j2 <= 7) &&
                        (j1 >= 6 || j2 >= 6) &&
                        diff >= 2 &&
                        !(j1 == 7 && j2 <= 5) &&
                        !(j2 == 7 && j1 <= 5);

        // ✅ 2. TIE-BREAK (7-6 con TB)
        boolean tieBreakValido =
                ((j1 == 7 && j2 == 6) || (j1 == 6 && j2 == 7)) &&
                        tieBreak >= 5;

        // ✅ 3. SUPER TIE-BREAK (10-8, 11-9, etc.)
        boolean superTieBreak =
                (j1 >= 10 || j2 >= 10) &&
                        diff >= 2 &&
                        (j1 >= 9 && j2 >= 9);

        if (!(setNormal || tieBreakValido || superTieBreak)) {
            throw new IllegalArgumentException("Marcador inválido: " + j1 + "-" + j2);
        }
    }

    @Override
    public String toString() {
        if (tieBreak > 0 &&
                ((juegosJ1 == 7 && juegosJ2 == 6) || (juegosJ2 == 7 && juegosJ1 == 6))) {
            return juegosJ1 + "-" + juegosJ2 + "(" + tieBreak + ")";
        }
        return juegosJ1 + "-" + juegosJ2;
    }
}