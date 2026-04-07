package com.tomas.tenis.stats.strategy.tournament.factory;

import com.tomas.tenis.stats.model.Partido;
import com.tomas.tenis.stats.model.TipoTorneo;
import com.tomas.tenis.stats.strategy.tournament.TournamentStrategy;
import com.tomas.tenis.stats.strategy.tournament.AtpChallengerStrategy;
import com.tomas.tenis.stats.strategy.tournament.GrandSlamStrategy;
import org.springframework.stereotype.Component;

@Component
public class TournamentStrategyFactory {

    private final GrandSlamStrategy grandSlamStrategy;
    private final AtpChallengerStrategy atpChallengerStrategy;

    public TournamentStrategyFactory(GrandSlamStrategy grandSlamStrategy,
                                     AtpChallengerStrategy atpChallengerStrategy) {
        this.grandSlamStrategy = grandSlamStrategy;
        this.atpChallengerStrategy = atpChallengerStrategy;
    }

    public TournamentStrategy getStrategy(Partido partido) {

        TipoTorneo tipo = partido.getCategoria().getTipo();

        switch (tipo) {
            case GRAND_SLAM:
                return grandSlamStrategy;

            case M1000:
            case ATP:
            case CHALLENGER:
                return atpChallengerStrategy;

            default:
                throw new IllegalArgumentException("Tipo no soportado");
        }
    }
}