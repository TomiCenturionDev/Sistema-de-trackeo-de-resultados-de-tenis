package com.tomas.tenis.stats.controller;

import com.tomas.tenis.stats.model.Jugador;
import com.tomas.tenis.stats.service.JugadorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para manejar las operaciones de los Jugadores.
 */
@RestController
@RequestMapping("/api/jugadores")
public class JugadorController {

    private final JugadorService jugadorService;

    public JugadorController(JugadorService jugadorService) {
        this.jugadorService = jugadorService;
    }

    @GetMapping
    public List<Jugador> obtenerJugadores() {
        return jugadorService.listarTodosLosJugadores();
    }

    @PostMapping
    public Jugador crearNuevoJugador(@RequestBody Jugador jugador) {
        return jugadorService.guardarNuevoJugador(jugador);
    }
}