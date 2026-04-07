package com.tomas.tenis.stats.service;

import com.tomas.tenis.stats.model.*;
import com.tomas.tenis.stats.repository.*;
import com.tomas.tenis.stats.salesforce.service.SalesforceDataService;
import com.tomas.tenis.stats.soap.CountryInfoServiceSoapType;
import com.tomas.tenis.stats.strategy.tournament.TournamentStrategy;
import com.tomas.tenis.stats.strategy.tournament.factory.TournamentStrategyFactory;
import com.tomas.tenis.stats.util.ExternalIdGenerator;
import com.tomas.tenis.stats.util.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.List;

@Service
public class PartidoService {

    private final PartidoRepository partidoRepository;
    private final CategoriaRepository categoriaRepository;
    private final JugadorRepository jugadorRepository;
    private final SalesforceDataService salesforceService;
    private final CountryInfoServiceSoapType countryInfoService;
    private final RetryPolicy retryPolicy;
    private final ExternalIdGenerator externalIdGenerator;
    @Autowired
    private TournamentStrategyFactory strategyFactory;

    private static final String SCORE_INICIAL = "0-0";
    private static final String GANADOR_PENDIENTE = "TBD";

    private static final Logger log = LoggerFactory.getLogger(PartidoService.class);

    public PartidoService(PartidoRepository partidoRepository, CategoriaRepository categoriaRepository,
                          JugadorRepository jugadorRepository, SalesforceDataService salesforceService,
                          CountryInfoServiceSoapType countryInfoService, RetryPolicy retryPolicy,
                          ExternalIdGenerator externalIdGenerator, TournamentStrategyFactory strategyFactory){
        this.partidoRepository = partidoRepository;
        this.categoriaRepository = categoriaRepository;
        this.jugadorRepository= jugadorRepository;
        this.salesforceService= salesforceService;
        this.countryInfoService= countryInfoService;
        this.retryPolicy = retryPolicy;
        this.externalIdGenerator= externalIdGenerator;
        this.strategyFactory = strategyFactory;
    }

    private void validarCantidadPorFase(String torneo, int year, FaseTorneo fase) {

        int limite = obtenerLimitePorFase(fase);

        long cantidad = partidoRepository.countByTorneoAndYearAndFase(torneo, year, fase);

        if (cantidad >= limite) {
            throw new IllegalStateException(
                    "Se superó el límite de partidos para la fase " + fase
            );
        }
    }

    private int obtenerLimitePorFase(FaseTorneo fase) {
        return switch (fase) {
            case F -> 1;
            // FUTURO; Para hacerlo más escalable con fases anteriores a la final
            // case SF -> 2;
            // case QF -> 4;
            default -> Integer.MAX_VALUE;
        };
    }

    @Transactional
    public Partido registrarPartidoCompleto(String nombreTorneo, Long categoriaId,
                                            String codigoPais, String superficie,
                                            Long jugador1Id, Long jugador2Id,
                                            String ciudadManual, FaseTorneo fase,
                                            String scoreInicial, String fechaJson,
                                            EstadoPartido estadoJson, String jugadorRetirado) {

        Categoria cat = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        Jugador j1 = jugadorRepository.findById(jugador1Id)
                .orElseThrow(() -> new RuntimeException("Jugador 1 no encontrado"));

        Jugador j2 = jugadorRepository.findById(jugador2Id)
                .orElseThrow(() -> new RuntimeException("Jugador 2 no encontrado"));

        String sedeFinal = determinarSede(codigoPais, ciudadManual);
        LocalDate fechaFinal = (fechaJson != null) ? LocalDate.parse(fechaJson) : LocalDate.now();

        int year = fechaFinal.getYear();
        validarCantidadPorFase(nombreTorneo, year, fase);



        Jugador jugadorA;
        Jugador jugadorB;
        Long jugadorAId;
        Long jugadorBId;

        if (jugador1Id < jugador2Id) {
            jugadorA = j1;
            jugadorB = j2;
            jugadorAId = jugador1Id;
            jugadorBId = jugador2Id;
        } else {
            jugadorA = j2;
            jugadorB = j1;
            jugadorAId = jugador2Id;
            jugadorBId = jugador1Id;
        }

        log.info("Registrando partido torneo={} jugadores={} vs {} fecha={}",
                nombreTorneo, jugadorAId, jugadorBId, fechaFinal);

        boolean existe = partidoRepository
                .findByTorneoAndFechaAndFaseAndJugador1IdAndJugador2Id(
                        nombreTorneo,
                        fechaFinal,
                        fase,
                        jugadorAId,
                        jugadorBId
                ).isPresent();

        if (existe) {
            throw new IllegalStateException("El partido ya existe para esos jugadores");
        }

        Partido partido = new Partido(fase);

        partido.setTorneo(nombreTorneo);
        partido.setCiudad(sedeFinal);
        partido.setPais(codigoPais);
        partido.setSuperficie(Superficie.valueOf(superficie.toUpperCase()));
        partido.setCategoria(cat);
        partido.setJugador1(jugadorA);
        partido.setJugador2(jugadorB);
        partido.setFecha(fechaFinal);
        partido.setJugadorRetirado(jugadorRetirado);

        procesarMarcadorYEstado(partido, scoreInicial);

        if (estadoJson != null && partido.getEstado() == EstadoPartido.PROGRAMADO) {
            partido.setEstado(estadoJson);
        }

        try {
            Partido partidoGuardado = partidoRepository.save(partido);
            partidoRepository.flush();
            sincronizarSalesforce(partidoGuardado);

            return partidoGuardado;

        } catch (DataIntegrityViolationException error) {

            log.warn("Intento duplicado de partido torneo={} fecha={} jugadores={} vs {}",
                    nombreTorneo, fechaFinal, jugadorAId, jugadorBId);

            throw new IllegalStateException("El partido ya existe (idempotencia DB)", error);
        }
    }

    @Transactional
    public Partido actualizarResultado(Long id, String nuevoScore) {
        Partido partido = partidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partido no encontrado"));

        procesarMarcadorYEstado(partido, nuevoScore);

        Partido actualizado = partidoRepository.save(partido);
        sincronizarSalesforce(actualizado);

        return actualizado;
    }

    public List<Partido> listarPartidos() {
        return partidoRepository.findAll();
    }

    public void eliminarPartido(Long id) {
        if (!partidoRepository.existsById(id)) {
            throw new RuntimeException("No existe el partido con ID: " + id);
        }
        partidoRepository.deleteById(id);
    }

    private void procesarMarcadorYEstado(Partido partido, String score) {

        if (score == null || score.trim().isEmpty()) {
            partido.setResultado(SCORE_INICIAL);
            partido.setGanador(GANADOR_PENDIENTE);
            return;
        }

        TournamentStrategy strategy = strategyFactory.getStrategy(partido);

        String scoreUpperOriginal = score.toUpperCase();

        // 🔥 CASOS BORDE → ahora usan Strategy
        if (scoreUpperOriginal.contains("RET") ||
                scoreUpperOriginal.contains("DEF") ||
                scoreUpperOriginal.contains("SUSP")) {

            partido.setResultado(score.trim());

            if (scoreUpperOriginal.contains("RET")) {
                partido.setEstado(EstadoPartido.RETIRO);
                partido.setGanador(strategy.determinarGanadorEspecial(partido));

            } else if (scoreUpperOriginal.contains("DEF")) {
                partido.setEstado(EstadoPartido.DESCALIFICACION);
                partido.setGanador(strategy.determinarGanadorEspecial(partido));

            } else if (scoreUpperOriginal.contains("SUSP")) {
                partido.setEstado(EstadoPartido.SUSPENDIDO);
                partido.setGanador(GANADOR_PENDIENTE);
            }

            return;
        }

        String scoreNormalizado = normalizarFormato(score);
        partido.setResultado(scoreNormalizado);

        String[] sets = scoreNormalizado.trim().split("\\s+");

        for (String set : sets) {
            if (!set.matches("\\d+-\\d+")) {
                throw new IllegalArgumentException("Formato de set inválido: " + set);
            }
        }

        strategy.validarCantidadSets(partido, sets);

        String ganador = strategy.determinarGanador(partido, sets);

        if (ganador.equals(GANADOR_PENDIENTE)) {
            partido.setGanador(GANADOR_PENDIENTE);
            partido.setEstado(EstadoPartido.EN_CURSO);
            return;
        }

        partido.setGanador(ganador);
        partido.setEstado(EstadoPartido.FINALIZADO);
    }


    private String normalizarFormato(String score) {
        if (score == null || score.trim().isEmpty()) return SCORE_INICIAL;

        String soloNumeros = score.replaceAll("[^0-9]", " ").trim().replaceAll("\\s+", " ");
        String[] partes = soloNumeros.split(" ");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.length; i++) {
            sb.append(partes[i]);
            if (i < partes.length - 1) {
                sb.append((i % 2 == 0) ? "-" : " ");
            }
        }
        return sb.toString();
    }

    private String determinarSede(String pais, String ciudad) {
        if (ciudad != null && !ciudad.isEmpty()) return ciudad;
        try {
            return countryInfoService.capitalCity(pais);
        } catch (Exception e) {
            return "Sede no definida";
        }
    }

    private void sincronizarSalesforce(Partido partido) {
        if (partido == null) return;

        // 🔥 1. Idempotencia a nivel aplicación
        if (partido.getSyncStatus() == SyncStatus.SUCCESS) {
            log.info("Partido ya sincronizado, evitando duplicado partidoId={}", partido.getId());
            return;
        }

        // 🔥 2. Generar externalId SIEMPRE antes del sync
        if (partido.getExternalId() == null) {
            partido.setExternalId(externalIdGenerator.generarExternalId(partido));

            // 🔥 IMPORTANTE: persistirlo inmediatamente
            partidoRepository.save(partido);
        }

        if (partido.getEstado() == EstadoPartido.FINALIZADO ||
                partido.getEstado() == EstadoPartido.RETIRO ||
                partido.getEstado() == EstadoPartido.DESCALIFICACION) {

            log.info("Iniciando sync partidoId={} estado={} externalId={}",
                    partido.getId(),
                    partido.getEstado(),
                    partido.getExternalId());

            try {
                salesforceService.enviarPartidoASalesforce(partido, partido.getExternalId());

                partido.setSyncStatus(SyncStatus.SUCCESS);

                log.info("Sync OK partidoId={} status={}",
                        partido.getId(),
                        partido.getSyncStatus());

            } catch (Exception e) {

                if (retryPolicy.esErrorReintentable(e)) {

                    partido.setSyncStatus(SyncStatus.FAILED);
                    partido.setRetryCount(partido.getRetryCount() + 1);

                    log.warn("Error REINTENTABLE sync partidoId={} retryCount={}",
                            partido.getId(),
                            partido.getRetryCount(),
                            e);

                } else {

                    partido.setSyncStatus(SyncStatus.FAILED_FINAL);

                    log.error("Error NO REINTENTABLE. No se reintentará. partidoId={}",
                            partido.getId(),
                            e);
                }
            }

            partidoRepository.save(partido);
        }
    }
}