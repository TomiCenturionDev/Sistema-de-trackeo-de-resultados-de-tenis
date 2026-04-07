package com.tomas.tenis.stats.strategy.tournament;

import com.tomas.tenis.stats.model.Partido;
import org.springframework.stereotype.Component;

@Component
public class GrandSlamStrategy implements TournamentStrategy {

    @Override
    public void validarCantidadSets(Partido partido, String[] sets) {
        if (sets.length < 3 || sets.length > 5) {
            throw new IllegalArgumentException("Grand Slam: 3 a 5 sets");
        }
    }

    @Override
    public int obtenerSetsParaGanar() {
        return 3;
    }

    @Override
    public String determinarGanador(Partido partido, String[] sets) {
        int j1 = 0;
        int j2 = 0;

        for (String set : sets) {
            String[] games = set.split("-");
            int g1 = Integer.parseInt(games[0]);
            int g2 = Integer.parseInt(games[1]);

            if (g1 > g2) j1++;
            else j2++;
        }

        return j1 == 3 ? partido.getJugador1().getNombre()
                : partido.getJugador2().getNombre();
    }

    @Override
    public String determinarGanadorEspecial(Partido partido) {

        // 🔒 Defensa contra null
        if (partido.getJugadorRetirado() == null) {
            throw new IllegalStateException("Jugador retirado no informado para partido en RETIRO");
        }

        if (java.util.Objects.equals(partido.getJugadorRetirado(), partido.getJugador1().getNombre())) {
            return partido.getJugador2().getNombre();
        } else {
            return partido.getJugador1().getNombre();
        }
    }
}

