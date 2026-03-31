package com.tomas.tenis.stats.service;

import com.tomas.tenis.stats.model.*;
import com.tomas.tenis.stats.repository.*;
import com.tomas.tenis.stats.salesforce.service.SalesforceDataService;
import com.tomas.tenis.stats.soap.CountryInfoService;
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

    private static final String SCORE_INICIAL = "0-0";
    private static final String GANADOR_PENDIENTE = "TBD";

    public PartidoService(PartidoRepository partidoRepository, CategoriaRepository categoriaRepository,
                          JugadorRepository jugadorRepository, SalesforceDataService salesforceService){
        this.partidoRepository = partidoRepository;
        this.categoriaRepository = categoriaRepository;
        this.jugadorRepository= jugadorRepository;
        this.salesforceService= salesforceService;
    }

    @Transactional
    public Partido registrarPartidoCompleto(String nombreTorneo, Long categoriaId,
                                            String codigoPais, String superficie,
                                            Long jugador1Id, Long jugador2Id,
                                            String ciudadManual, FaseTorneo fase,
                                            String scoreInicial, String fechaJson,
                                            EstadoPartido estadoJson
                                            ) {

                                             Categoria cat = categoriaRepository.findById(categoriaId)
                                                                                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
                                             Jugador j1 = jugadorRepository.findById(jugador1Id)
                                                                           .orElseThrow(() -> new RuntimeException("Jugador 1 no encontrado"));
                                             Jugador j2 = jugadorRepository.findById(jugador2Id)
                                                                            .orElseThrow(() -> new RuntimeException("Jugador 2 no encontrado"));
                                            String sedeFinal = determinarSede(codigoPais, ciudadManual);

        // Parseamos la fecha que viene de Thunder Client / Postman
        LocalDate fechaFinal = (fechaJson != null) ? LocalDate.parse(fechaJson) : LocalDate.now();

        //  Buscamos si ya existe este partido para evitar duplicar IDs (2, 3, 4...)
        // Si existe, lo recuperamos para actualizarlo; si no, creamos uno nuevo.
        Partido partido = partidoRepository.findByTorneoAndFechaAndFase(nombreTorneo, fechaFinal, fase)
                .orElse(new Partido(fase));

        partido.setTorneo(nombreTorneo);
        partido.setCiudad(sedeFinal);
        partido.setPais(codigoPais);
        partido.setSuperficie(Superficie.valueOf(superficie.toUpperCase()));
        partido.setCategoria(cat);
        partido.setJugador1(j1);
        partido.setJugador2(j2);
        partido.setFecha(fechaFinal); // <--- Atributo de fecha asignado

        procesarMarcadorYEstado(partido, scoreInicial);

        if (estadoJson != null) {
            partido.setEstado(estadoJson);
        }

        // Si el partido ya existía, Hibernate hará un UPDATE; si no, un INSERT.
        Partido partidoGuardado = partidoRepository.save(partido);
        sincronizarSalesforce(partidoGuardado);

        return partidoGuardado;
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

    // --- LÓGICA DE PROCESAMIENTO REESTABLECIDA ---

    private void procesarMarcadorYEstado(Partido partido, String score) {
        if (score == null || score.trim().isEmpty()) {
            partido.setResultado(SCORE_INICIAL);
            partido.setGanador(GANADOR_PENDIENTE);
            return;
        }

        String scoreNormalizado = normalizarFormato(score);
        String scoreUpper = scoreNormalizado.toUpperCase().trim();
        partido.setResultado(scoreNormalizado);

        // 🛡️ GESTIÓN DE CASOS ESPECIALES
        if (scoreUpper.contains("RET") || scoreUpper.contains("DEF") || scoreUpper.contains("SUSP")) {
            if (scoreUpper.contains("RET")) {
                partido.setEstado(EstadoPartido.RETIRO);
                partido.setGanador(partido.getJugador1().getNombre());
            } else if (scoreUpper.contains("DEF")) {
                partido.setEstado(EstadoPartido.DESCALIFICACION);
                partido.setGanador(partido.getJugador1().getNombre());
            } else if (scoreUpper.contains("SUSP")) {
                partido.setEstado(EstadoPartido.SUSPENDIDO);
                partido.setGanador(GANADOR_PENDIENTE);
            }
            return; // 🚀 Cortamos la ejecución aquí para evitar el conteo de sets
        }

        // --- LÓGICA DE ANÁLISIS DE JUEGOS ---
        String scoreLimpio = scoreNormalizado.trim();
        String[] sets = scoreLimpio.split("\\s+");

        int setsJ1 = 0;
        int setsJ2 = 0;

        for (String set : sets) {
            try {
                String[] juegos = set.split("-");
                if (juegos.length != 2) continue;

                int j1 = Integer.parseInt(juegos[0].trim());
                int j2 = Integer.parseInt(juegos[1].trim());

                if ((j1 >= 6 && (j1 - j2 >= 2)) || (j1 == 7 && (j2 == 5 || j2 == 6))) {
                    setsJ1++;
                } else if ((j2 >= 6 && (j2 - j1 >= 2)) || (j2 == 7 && (j1 == 5 || j1 == 6))) {
                    setsJ2++;
                }
            } catch (NumberFormatException e) {
                System.err.println("Error procesando set: " + set);
            }
        }

        int setsNecesarios = (partido.getCategoria().getPuntos() >= 2000) ? 3 : 2;

        if (setsJ1 == setsNecesarios) {
            partido.setGanador(partido.getJugador1().getNombre());
            partido.setEstado(EstadoPartido.FINALIZADO);
        } else if (setsJ2 == setsNecesarios) {
            partido.setGanador(partido.getJugador2().getNombre());
            partido.setEstado(EstadoPartido.FINALIZADO);
        } else {
            partido.setGanador(GANADOR_PENDIENTE);
            partido.setEstado(EstadoPartido.EN_CURSO);
        }
    }

    // Metodo auxiliar para que el parser entienda los espacios como guiones entre juegos
    private String normalizarFormato(String score) {
        if (score == null || score.trim().isEmpty()) return SCORE_INICIAL;

        // 1. Quitamos todos los guiones y espacios extra para tener solo números
        String soloNumeros = score.replaceAll("[^0-9]", " ").trim().replaceAll("\\s+", " ");
        String[] partes = soloNumeros.split(" ");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.length; i++) {
            sb.append(partes[i]);
            if (i < partes.length - 1) {
                sb.append((i % 2 == 0) ? "-" : " ");
            }
        }
        return sb.toString(); // Devolverá por ejemplo: "6-4 7-6"
    }

    private String determinarSede(String pais, String ciudad) {
        if (ciudad != null && !ciudad.isEmpty()) return ciudad;
        try {
            return new CountryInfoService().getCountryInfoServiceSoap().capitalCity(pais);
        } catch (Exception e) {
            return "Sede no definida";
        }
    }

    private void sincronizarSalesforce(Partido p) {
        // Solo sincronizamos si el partido tiene un estado final
        if (p.getEstado() == EstadoPartido.FINALIZADO ||
                p.getEstado() == EstadoPartido.RETIRO ||
                p.getEstado() == EstadoPartido.DESCALIFICACION) {
            try {
                salesforceService.enviarPartidoASalesforce(p);
                System.out.println("✅ [SALESFORCE] Sincronización exitosa del partido ID: " + p.getId());
            } catch (Exception e) {
                // ❌ LOG DE REBOTE: Aquí verás por qué Salesforce rechazó el paquete
                System.err.println("=========================================");
                System.err.println("❌ [SALESFORCE] El envío REBOTÓ para el partido ID: " + p.getId());
                System.err.println("MOTIVO: " + e.getMessage());
                System.err.println("=========================================");
            }
        }
    }
}