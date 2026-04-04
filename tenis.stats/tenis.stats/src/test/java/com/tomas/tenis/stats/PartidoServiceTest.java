package com.tomas.tenis.stats;

import com.tomas.tenis.stats.model.*;
import com.tomas.tenis.stats.repository.CategoriaRepository;
import com.tomas.tenis.stats.repository.JugadorRepository;
import com.tomas.tenis.stats.repository.PartidoRepository;
import com.tomas.tenis.stats.salesforce.service.SalesforceDataService;
import com.tomas.tenis.stats.service.PartidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PartidoServiceTest {

    @Mock
    private PartidoRepository partidoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private JugadorRepository jugadorRepository;

    @Mock
    private SalesforceDataService salesforceService;

    @InjectMocks
    private PartidoService partidoService;

    @BeforeEach
    void setUp() {

        Categoria categoria = new Categoria();
        categoria.setPuntos(1000);

        Jugador j1 = new Jugador();
        j1.setNombre("Jugador 1");

        Jugador j2 = new Jugador();
        j2.setNombre("Jugador 2");

        // 👉 lenient para evitar UnnecessaryStubbingException
        lenient().when(categoriaRepository.findById(anyLong()))
                .thenReturn(Optional.of(categoria));

        lenient().when(jugadorRepository.findById(1L))
                .thenReturn(Optional.of(j1));

        lenient().when(jugadorRepository.findById(2L))
                .thenReturn(Optional.of(j2));

        lenient().when(partidoRepository.findByTorneoAndFechaAndFaseAndJugador1IdAndJugador2Id(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        lenient().when(salesforceService.enviarPartidoASalesforce(any()))
                .thenReturn("OK");
    }

    // ✅ helper limpio y reutilizable
    private void mockSave() {
        when(partidoRepository.save(any())).thenAnswer(inv -> {
            Partido p = inv.getArgument(0);
            if (p == null) {
                p = new Partido(); // 🔥 evita NPE
            }
            p.setId(1L);
            return p;
        });
    }

    @Test
    public void deberiaFallarSiGrandSlamTieneMenosDe3Sets() {

        Categoria categoria = new Categoria();
        categoria.setPuntos(2000);

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        assertThrows(IllegalArgumentException.class, () -> {
            partidoService.registrarPartidoCompleto(
                    "Roland Garros",
                    1L,
                    "FR",
                    "ARCILLA",
                    1L,
                    2L,
                    "Paris",
                    FaseTorneo.F,
                    "6-3 6-4",
                    "2024-06-09",
                    EstadoPartido.FINALIZADO
            );
        });
    }

    @Test
    public void deberiaAceptarGrandSlamCon3Sets() {

        mockSave();

        Categoria categoria = new Categoria();
        categoria.setPuntos(2000);

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        assertDoesNotThrow(() -> {
            partidoService.registrarPartidoCompleto(
                    "Roland Garros",
                    1L,
                    "FR",
                    "ARCILLA",
                    1L,
                    2L,
                    "Paris",
                    FaseTorneo.F,
                    "6-3 4-6 6-2",
                    "2024-06-09",
                    EstadoPartido.FINALIZADO
            );
        });
    }

    @Test
    public void deberiaFallarSiATPTieneMasDe3Sets() {

        assertThrows(IllegalArgumentException.class, () -> {
            partidoService.registrarPartidoCompleto(
                    "Roma",
                    1L,
                    "IT",
                    "ARCILLA",
                    1L,
                    2L,
                    "Roma",
                    FaseTorneo.R32,
                    "6-3 4-6 6-2 6-1",
                    "2024-05-15",
                    EstadoPartido.FINALIZADO
            );
        });
    }

    @Test
    public void deberiaProcesarCorrectamentePartidoConRetiro() {

        mockSave();

        Partido resultado = partidoService.registrarPartidoCompleto(
                "Roma",
                1L,
                "IT",
                "ARCILLA",
                1L,
                2L,
                "Roma",
                FaseTorneo.R32,
                "6-3 1-0 RET",
                "2024-05-15",
                EstadoPartido.RETIRO
        );

        assertAll(
                () -> assertEquals(EstadoPartido.RETIRO, resultado.getEstado()),
                () -> assertEquals("Jugador 1", resultado.getGanador()),
                () -> assertEquals("6-3 1-0 RET", resultado.getResultado())
        );
    }

    @Test
    public void deberiaDejarPartidoPendienteSiScoreEsNull() {

        mockSave();

        Partido resultado = partidoService.registrarPartidoCompleto(
                "Roma",
                1L,
                "IT",
                "ARCILLA",
                1L,
                2L,
                "Roma",
                FaseTorneo.R32,
                null,
                "2024-05-15",
                null
        );

        assertEquals("0-0", resultado.getResultado());
        assertEquals("TBD", resultado.getGanador());
    }

    @Test
    public void deberiaQuedarEnCursoSiNoSeCompletoElPartido() {

        mockSave();

        Partido resultado = partidoService.registrarPartidoCompleto(
                "Roma",
                1L,
                "IT",
                "ARCILLA",
                1L,
                2L,
                "Roma",
                FaseTorneo.R32,
                "6-3 2-2",
                "2024-05-15",
                null
        );

        assertEquals(EstadoPartido.EN_CURSO, resultado.getEstado());
    }
    @Test
    public void deberiaFallarSiCategoriaNoExiste() {

        when(categoriaRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            partidoService.registrarPartidoCompleto(
                    "Roma",
                    1L,
                    "IT",
                    "ARCILLA",
                    1L,
                    2L,
                    "Roma",
                    FaseTorneo.R32,
                    "6-3 6-4",
                    "2024-05-15",
                    EstadoPartido.FINALIZADO
            );
        });
    }
    @Test
    void deberiaFallarSiJugadorNoExiste() {

        when(jugadorRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            partidoService.registrarPartidoCompleto(
                    "Roma",
                    1L,
                    "IT",
                    "ARCILLA",
                    1L,
                    2L,
                    "Roma",
                    FaseTorneo.R32,
                    "6-3 6-4",
                    "2024-05-15",
                    EstadoPartido.FINALIZADO
            );
        });
    }
    @Test
    public void deberiaFallarSiElPartidoYaExiste() {

        when(partidoRepository.findByTorneoAndFechaAndFaseAndJugador1IdAndJugador2Id(any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(new Partido()));

        assertThrows(RuntimeException.class, () -> {
            partidoService.registrarPartidoCompleto(
                    "Roma",
                    1L,
                    "IT",
                    "ARCILLA",
                    1L,
                    2L,
                    "Roma",
                    FaseTorneo.R32,
                    "6-3 6-4",
                    "2024-05-15",
                    EstadoPartido.FINALIZADO
            );
        });
    }

    @Test
    public void deberiaDeterminarGanadorCorrectamente() {

        mockSave();

        Partido resultado = partidoService.registrarPartidoCompleto(
                "Roma",
                1L,
                "IT",
                "ARCILLA",
                1L,
                2L,
                "Roma",
                FaseTorneo.R32,
                "6-3 4-6 6-2",
                "2024-05-15",
                EstadoPartido.FINALIZADO
        );

        assertEquals("Jugador 1", resultado.getGanador());
    }

    @Test
    public void deberiaFallarSiScoreEsInvalido() {

        assertThrows(IllegalArgumentException.class, () -> {
            partidoService.registrarPartidoCompleto(
                    "Roma",
                    1L,
                    "IT",
                    "ARCILLA",
                    1L,
                    2L,
                    "Roma",
                    FaseTorneo.R32,
                    "abc", // inválido
                    "2024-05-15",
                    EstadoPartido.FINALIZADO
            );
        });
    }
    @Test
    public void noDeberiaGuardarSiElPartidoYaExiste() {

        Partido existente = new Partido();

        when(partidoRepository.findByTorneoAndFechaAndFaseAndJugador1IdAndJugador2Id(any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(existente));

        assertThrows(IllegalStateException.class, () -> {
            partidoService.registrarPartidoCompleto(
                    "Roma", 1L, "IT", "ARCILLA",
                    1L, 2L, "Roma",
                    FaseTorneo.R32,
                    "6-3 6-4",
                    "2024-05-15",
                    EstadoPartido.FINALIZADO
            );
        });
    }

    @Test
    public void noDeberiaPermitirJugadoresInvertidos() {

        when(partidoRepository
                .findByTorneoAndFechaAndFaseAndJugador1IdAndJugador2Id(
                        any(), any(), any(), eq(1L), eq(2L)
                ))
                .thenReturn(Optional.of(new Partido()));

        assertThrows(IllegalStateException.class, () -> {
            partidoService.registrarPartidoCompleto(
                    "Roma", 1L, "IT", "ARCILLA",
                    2L, 1L, // 👈 invertidos en input
                    "Roma",
                    FaseTorneo.R32,
                    "6-3 6-4",
                    "2024-05-15",
                    EstadoPartido.FINALIZADO
            );
        });
    }

    @Test
    public void deberiaNormalizarScoreCorrectamente() {
        mockSave();

        Partido resultado = partidoService.registrarPartidoCompleto(
                "Roma", 1L, "IT", "ARCILLA",
                1L, 2L, "Roma",
                FaseTorneo.R32,
                " 6-3   4-6  6-2 ",
                "2024-05-15",
                EstadoPartido.FINALIZADO
        );
        assertEquals("6-3 4-6 6-2", resultado.getResultado());
    }
    @Test
    public void noDeberiaSobrescribirEstadoSiYaFueCalculado() {

        mockSave();

        Partido resultado = partidoService.registrarPartidoCompleto(
                "Roma", 1L, "IT", "ARCILLA",
                1L, 2L, "Roma",
                FaseTorneo.R32,
                "6-3 6-4", // ya define FINALIZADO
                "2024-05-15",
                EstadoPartido.EN_CURSO // ❌ no debería pisar
        );

        assertEquals(EstadoPartido.FINALIZADO, resultado.getEstado());
    }

    @Test
    public void deberiaProcesarPartidoSuspendido() {

        mockSave();

        Partido resultado = partidoService.registrarPartidoCompleto(
                "Roma", 1L, "IT", "ARCILLA",
                1L, 2L, "Roma",
                FaseTorneo.R32,
                "6-3 2-2 SUSP",
                "2024-05-15",
                null
        );

        assertAll(
                () -> assertEquals(EstadoPartido.SUSPENDIDO, resultado.getEstado()),
                () -> assertEquals("TBD", resultado.getGanador())
        );
    }
    @Test
    public void deberiaContarSetConTiebreakCorrectamente() {

        mockSave();

        Partido resultado = partidoService.registrarPartidoCompleto(
                "Roma", 1L, "IT", "ARCILLA",
                1L, 2L, "Roma",
                FaseTorneo.R32,
                "7-6 6-4",
                "2024-05-15",
                EstadoPartido.FINALIZADO
        );

        assertEquals("Jugador 1", resultado.getGanador());
    }
    @Test
    public void noDeberiaSincronizarSiPartidoNoEstaFinalizado() {

        mockSave();

        partidoService.registrarPartidoCompleto(
                "Roma", 1L, "IT", "ARCILLA",
                1L, 2L, "Roma",
                FaseTorneo.R32,
                "6-3 2-2", // en curso
                "2024-05-15",
                null
        );

        verify(salesforceService, never()).enviarPartidoASalesforce(any());
    }

    @Test
    public void noDeberiaRomperSiFallaSalesforce() {

        mockSave();

        // 🔥 simulamos fallo externo
        when(salesforceService.enviarPartidoASalesforce(any()))
                .thenThrow(new RuntimeException("Salesforce caído"));

        Partido resultado = partidoService.registrarPartidoCompleto(
                "Roma",
                1L,
                "IT",
                "ARCILLA",
                1L,
                2L,
                "Roma",
                FaseTorneo.R32,
                "6-3 6-4",
                "2024-05-15",
                EstadoPartido.FINALIZADO
        );

        // ✔ el sistema sigue funcionando
        assertNotNull(resultado);
        assertEquals("Jugador 1", resultado.getGanador());
        verify(salesforceService, times(1))
                .enviarPartidoASalesforce(any());
    }
}
