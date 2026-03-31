package com.tomas.tenis.stats.service;

import com.tomas.tenis.stats.model.Jugador;
import com.tomas.tenis.stats.repository.JugadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JugadorService {

    @Autowired
    private JugadorRepository jugadorRepository;

    public List<Jugador> listarTodosLosJugadores() {
        return jugadorRepository.findAll();
    }

    public Jugador guardarNuevoJugador(Jugador jugador) {
        return jugadorRepository.save(jugador);
    }
}