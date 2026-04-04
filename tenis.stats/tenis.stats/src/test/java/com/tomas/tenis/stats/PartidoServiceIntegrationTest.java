package com.tomas.tenis.stats;

import com.tomas.tenis.stats.model.*;
import com.tomas.tenis.stats.repository.CategoriaRepository;
import com.tomas.tenis.stats.repository.JugadorRepository;
import com.tomas.tenis.stats.service.PartidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        cat.setPuntos(1000);
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
                EstadoPartido.FINALIZADO
        );
        assertNotNull(resultado.getId());
        assertEquals("Jugador 1", resultado.getGanador());
    }
}