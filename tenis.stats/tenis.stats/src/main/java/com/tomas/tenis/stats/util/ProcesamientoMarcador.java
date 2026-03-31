package com.tomas.tenis.stats.util;

import com.tomas.tenis.stats.model.EstadoPartido;
import com.tomas.tenis.stats.model.Resultado;
import com.tomas.tenis.stats.model.ResultadoParcial;
import lombok.Getter;

import java.util.List;
import java.util.Optional;

public class ProcesamientoMarcador {

    @Getter
    private final List<Resultado> setsCompletos;
    @Getter
    private final EstadoPartido estadoSugerido;

    private final ResultadoParcial setIncompleto; // tipado
    private final String setIncompletoRaw;        // original

    public ProcesamientoMarcador(List<Resultado> setsCompletos,
                                 EstadoPartido estadoSugerido,
                                 ResultadoParcial setIncompleto,
                                 String setIncompletoRaw) {
        this.setsCompletos = setsCompletos;
        this.estadoSugerido = estadoSugerido;
        this.setIncompleto = setIncompleto;
        this.setIncompletoRaw = setIncompletoRaw;
    }

    public Optional<ResultadoParcial> getSetIncompleto() {

        return Optional.ofNullable(setIncompleto);
    }

    public Optional<String> getSetIncompletoRaw() {
        return Optional.ofNullable(setIncompletoRaw);
    }

    public boolean tieneSetIncompleto() {

        return setIncompleto != null;
    }
}