package com.tomas.tenis.stats.controller;

import com.tomas.tenis.stats.dto.PartidoRequest;
import com.tomas.tenis.stats.model.Partido;
import com.tomas.tenis.stats.service.PartidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus; // Agregado para el 201 Created
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/partidos")
public class PartidoController {

    private final PartidoService partidoService;
    private String jugadorRetirado;

    public PartidoController(PartidoService partidoService) {
        this.partidoService = partidoService;
    }


    @PostMapping("/registrar")
    public ResponseEntity<Partido> guardarNuevoPartido(@Valid @RequestBody PartidoRequest request) {

        // 🛠️ FIX: Ahora extraemos los datos de los objetos del DTO
        // y pasamos los parámetros de fecha y estado al Service.
        Partido partido = partidoService.registrarPartidoCompleto(
                request.getTorneo(),
                request.getCategoria().getId(), // Extraemos el ID del objeto Categoria
                request.getPais(),
                request.getSuperficie().name(), // .name() para pasarlo como String al Service
                request.getJugador1().getId(),  // Extraemos el ID del objeto Jugador 1
                request.getJugador2().getId(),  // Extraemos el ID del objeto Jugador 2
                request.getCiudad(),
                request.getFase(),
                request.getResultado(),
                request.getFecha(),             // Pasamos la fecha del JSON
                request.getEstado(),          // Pasamos el estado del JSON
                request.getJugadorRetirado()
        );

        return new ResponseEntity<>(partido, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/resultado")
    public ResponseEntity<Partido> actualizarMarcador(@PathVariable Long id,
                                                      @RequestBody Map<String, String> body) {
        String nuevoScore = body.get("resultado");
        Partido actualizado = partidoService.actualizarResultado(id, nuevoScore);
        return ResponseEntity.ok(actualizado);
    }

    @GetMapping("/listar")
    public ResponseEntity<List<Partido>> obtenerTodosLosPartidos() {
        List<Partido> partidos = partidoService.listarPartidos();
        return ResponseEntity.ok(partidos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminar(@PathVariable Long id) {
        partidoService.eliminarPartido(id);
        return ResponseEntity.ok("El partido con ID " + id + " ha sido eliminado exitosamente.");
    }
}