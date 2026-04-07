package com.tomas.tenis.stats.strategy.tournament;

import com.tomas.tenis.stats.model.Partido;

public interface TournamentStrategy {

        void validarCantidadSets(Partido partido, String[] sets);

        int obtenerSetsParaGanar();

        String determinarGanador(Partido partido, String[] sets);

        String determinarGanadorEspecial(Partido partido);
}