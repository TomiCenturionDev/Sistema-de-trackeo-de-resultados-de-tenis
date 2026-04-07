package com.tomas.tenis.stats;

import com.tomas.tenis.stats.model.*;
import com.tomas.tenis.stats.repository.CategoriaRepository;
import com.tomas.tenis.stats.repository.JugadorRepository;
import com.tomas.tenis.stats.repository.PartidoRepository;
import com.tomas.tenis.stats.salesforce.service.SalesforceDataService;
import com.tomas.tenis.stats.service.PartidoService;
import com.tomas.tenis.stats.strategy.tournament.TournamentStrategy;
import com.tomas.tenis.stats.strategy.tournament.factory.TournamentStrategyFactory;
import com.tomas.tenis.stats.util.ExternalIdGenerator;
import com.tomas.tenis.stats.util.RetryPolicy;
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

    @Mock
    private RetryPolicy retryPolicy;

    @Mock
    private ExternalIdGenerator externalIdGenerator;

    @Mock
    private TournamentStrategyFactory strategyFactory;

    @Mock
    private TournamentStrategy strategy;

    @InjectMocks
    private PartidoService partidoService;

    @BeforeEach
    public void setUp() {

        Categoria categoria = new Categoria();
        categoria.setPuntos(1000);
        categoria.setTipo(TipoTorneo.ATP); // 🔥 clave

        Jugador j1 = new Jugador();
        j1.setNombre("Jugador 1");

        Jugador j2 = new Jugador();
        j2.setNombre("Jugador 2");

        lenient().when(categoriaRepository.findById(anyLong()))
                .thenReturn(Optional.of(categoria));

        lenient().when(jugadorRepository.findById(1L))
                .thenReturn(Optional.of(j1));

        lenient().when(jugadorRepository.findById(2L))
                .thenReturn(Optional.of(j2));

        lenient().when(partidoRepository
                        .findByTorneoAndFechaAndFaseAndJugador1IdAndJugador2Id(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        lenient().when(salesforceService.enviarPartidoASalesforce(any(), anyString()))
                .thenReturn("OK");

        lenient().when(externalIdGenerator.generarExternalId(any()))
                .thenReturn("TEST_ID");

        lenient().when(strategyFactory.getStrategy(any()))
                .thenReturn(strategy);

        lenient().when(strategy.obtenerSetsParaGanar())
                .thenReturn(2);

        lenient().doNothing().when(strategy)
                .validarCantidadSets(any(), any());

        lenient().when(strategy.determinarGanador(any(), any()))
                .thenReturn("Jugador 1");

        lenient().when(strategy.determinarGanadorEspecial(any()))
                .thenReturn("Jugador 1");



        lenient().when(partidoRepository.save(any()))
                .thenAnswer(inv -> {
                    Partido p = inv.getArgument(0);
                    p.setId(1L);
                    return p;
                });
    }

    @Test
    public void deberiaProcesarCorrectamentePartidoFinalizado() {

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
                EstadoPartido.FINALIZADO,
                null
        );

        assertAll(
                () -> assertEquals("Jugador 1", resultado.getGanador()),
                () -> assertEquals(EstadoPartido.FINALIZADO, resultado.getEstado()),
                () -> assertEquals("6-3 4-6 6-2", resultado.getResultado())
        );
    }

    @Test
    public void deberiaProcesarRetiroCorrectamente() {

        when(strategy.determinarGanadorEspecial(any()))
                .thenReturn("Jugador 2");

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
                EstadoPartido.RETIRO,
                "2L"
        );

        assertEquals("Jugador 2", resultado.getGanador());
        assertEquals(EstadoPartido.RETIRO, resultado.getEstado());
    }

    @Test
    public void deberiaDejarPartidoPendienteSiScoreEsNull() {

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
                null,
                null
        );

        assertEquals("0-0", resultado.getResultado());
        assertEquals("TBD", resultado.getGanador());
    }

    @Test
    public void deberiaQuedarEnCursoSiPartidoIncompleto() {

        when(strategy.determinarGanador(any(), any()))
                .thenReturn("TBD");

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
                null,
                null
        );

        assertEquals(EstadoPartido.EN_CURSO, resultado.getEstado());
    }

    @Test
    public void noDeberiaSincronizarSiNoEstaFinalizado() {

        when(strategy.determinarGanador(any(), any()))
                .thenReturn("TBD");

        partidoService.registrarPartidoCompleto(
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
                null,
                null
        );

        verify(salesforceService, never())
                .enviarPartidoASalesforce(any(), anyString());
    }

    @Test
    public void deberiaManejarErrorDeSalesforce() {

        when(salesforceService.enviarPartidoASalesforce(any(), anyString()))
                .thenThrow(new RuntimeException());

        when(retryPolicy.esErrorReintentable(any()))
                .thenReturn(false);

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
                EstadoPartido.FINALIZADO,
                null
        );

        assertEquals(SyncStatus.FAILED_FINAL, resultado.getSyncStatus());
    }
}