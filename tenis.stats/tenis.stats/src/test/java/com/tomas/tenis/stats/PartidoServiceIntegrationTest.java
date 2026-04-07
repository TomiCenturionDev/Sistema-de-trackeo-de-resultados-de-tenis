package com.tomas.tenis.stats;

import com.tomas.tenis.stats.model.*;
import com.tomas.tenis.stats.repository.CategoriaRepository;
import com.tomas.tenis.stats.repository.JugadorRepository;
import com.tomas.tenis.stats.service.PartidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional

/**
 * Tests de integración que se ejecutan dentro de una transacción
 * con rollback automático al finalizar cada test.
 */
public class PartidoServiceIntegrationTest {

    @Autowired
    private PartidoService partidoService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private JugadorRepository jugadorRepository;


    /**
     * Integration test que valida el flujo completo de registro de un partido.
     *
     * Este test utiliza el contexto real de Spring (Service + Repository) y persiste
     * entidades en la base de datos dentro de una transacción de prueba.
     *
     * ⚠️ Importante:
     * - La transacción es automáticamente revertida (rollback) al finalizar el test.
     * - Por lo tanto, los datos NO quedan persistidos en la base de datos real.
     *
     * ✔ Qué valida este test:
     * - Que el servicio guarda correctamente el partido
     * - Que se asigna un ID
     * - Que la lógica de negocio (ganador) funciona correctamente
     *
     * ✔ Qué NO valida:
     * - Persistencia real permanente en la base de datos
     *
     * Este comportamiento es intencional y asegura que los tests sean aislados,
     * reproducibles y no contaminen el entorno de datos.
     */

    @Test
    public void deberiaGuardarPartidoRealEnDB() {

        Categoria cat = new Categoria();
        cat.setNombre("Masters 1000");
        cat.setPuntos(1000);
        cat.setTipoDeTorneo("ATP"); // 🔥 FIX CLAVE
        categoriaRepository.save(cat);

        Jugador j1 = new Jugador();
        j1.setNombre("Jugador 1");
        jugadorRepository.save(j1);

        Jugador j2 = new Jugador();
        j2.setNombre("Jugador 2");
        jugadorRepository.save(j2);

        Partido resultado = partidoService.registrarPartidoCompleto(
                "Roma",
                cat.getId(),
                "IT",
                "ARCILLA",
                j1.getId(),
                j2.getId(),
                "Roma",
                FaseTorneo.F,
                "6-3 6-4",
                "2024-05-15",
                EstadoPartido.FINALIZADO,
                null

        );

        assertNotNull(resultado.getId());
        assertEquals("Jugador 1", resultado.getGanador());
    }

    @Test
    public void noDeberiaPermitirMasDeUnaFinalPorAnio() {

        Categoria cat = new Categoria();
        cat.setNombre("Masters 1000");
        cat.setPuntos(1000);
        cat.setTipoDeTorneo("ATP");
        categoriaRepository.save(cat);

        Jugador j1 = new Jugador();
        j1.setNombre("Jugador 1");
        jugadorRepository.save(j1);

        Jugador j2 = new Jugador();
        j2.setNombre("Jugador 2");
        jugadorRepository.save(j2);

        // Primera final (OK)
        partidoService.registrarPartidoCompleto(
                "Roma",
                cat.getId(), // 🔥 FIX CLAVE
                "IT",
                "ARCILLA",
                j1.getId(), // 🔥 también acá
                j2.getId(),
                "Roma",
                FaseTorneo.F,
                "6-3 6-4",
                "2024-05-15",
                EstadoPartido.FINALIZADO,
                null
        );

        // Segunda final (debe fallar)
        Exception exception = assertThrows(IllegalStateException.class, () -> {

            Jugador j3 = new Jugador();
            j3.setNombre("Jugador 3");
            jugadorRepository.save(j3);

            Jugador j4 = new Jugador();
            j4.setNombre("Jugador 4");
            jugadorRepository.save(j4);

            partidoService.registrarPartidoCompleto(
                    "Roma",
                    cat.getId(), // 🔥 FIX
                    "IT",
                    "ARCILLA",
                    j3.getId(), // 🔥 FIX
                    j4.getId(),
                    "Roma",
                    FaseTorneo.F,
                    "6-2 6-2",
                    "2024-05-20",
                    EstadoPartido.FINALIZADO,
                    null
            );
        });

        assertEquals(
                "Se superó el límite de partidos para la fase F",
                exception.getMessage()
        );
    }
}