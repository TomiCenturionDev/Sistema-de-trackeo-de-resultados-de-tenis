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
                                            EstadoPartido estadoJson) {

        Categoria cat = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        Jugador j1 = jugadorRepository.findById(jugador1Id)
                .orElseThrow(() -> new RuntimeException("Jugador 1 no encontrado"));

        Jugador j2 = jugadorRepository.findById(jugador2Id)
                .orElseThrow(() -> new RuntimeException("Jugador 2 no encontrado"));

        String sedeFinal = determinarSede(codigoPais, ciudadManual);
        LocalDate fechaFinal = (fechaJson != null) ? LocalDate.parse(fechaJson) : LocalDate.now();

        // 🔥 NORMALIZACIÓN REAL (IDs + entidades)
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

        // 🔥 VALIDACIÓN DE DUPLICADOS (consistente con persistencia)
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

        // 🔥 IMPORTANTE: persistimos NORMALIZADO
        partido.setJugador1(jugadorA);
        partido.setJugador2(jugadorB);

        partido.setFecha(fechaFinal);

        // 🔥 lógica de negocio
        procesarMarcadorYEstado(partido, scoreInicial);

        if (estadoJson != null && partido.getEstado() == EstadoPartido.PROGRAMADO) {
            partido.setEstado(estadoJson);
        }

        Partido partidoGuardado = partidoRepository.save(partido);

        // 🔥 resiliente (no rompe flujo)
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

        String scoreUpperOriginal = score.toUpperCase();

        // 🔥 1. CASOS ESPECIALES
        if (scoreUpperOriginal.contains("RET") || scoreUpperOriginal.contains("DEF") || scoreUpperOriginal.contains("SUSP")) {

            partido.setResultado(score.trim());

            if (scoreUpperOriginal.contains("RET")) {
                partido.setEstado(EstadoPartido.RETIRO);
                partido.setGanador(partido.getJugador1().getNombre());
            } else if (scoreUpperOriginal.contains("DEF")) {
                partido.setEstado(EstadoPartido.DESCALIFICACION);
                partido.setGanador(partido.getJugador1().getNombre());
            } else if (scoreUpperOriginal.contains("SUSP")) {
                partido.setEstado(EstadoPartido.SUSPENDIDO);
                partido.setGanador(GANADOR_PENDIENTE);
            }

            return;
        }

        // 🔥 2. NORMALIZACIÓN
        String scoreNormalizado = normalizarFormato(score);
        partido.setResultado(scoreNormalizado);

        String[] sets = scoreNormalizado.trim().split("\\s+");

        // 🔥 VALIDACIÓN DE FORMATO
        for (String set : sets) {
            if (!set.matches("\\d+-\\d+")) {
                throw new IllegalArgumentException("Formato de set inválido: " + set);
            }
        }

        validarCantidadSets(partido, sets, scoreNormalizado);

        int setsJ1 = 0;
        int setsJ2 = 0;

        for (String set : sets) {

            String[] juegos = set.split("-");

            try {
                int j1 = Integer.parseInt(juegos[0].trim());
                int j2 = Integer.parseInt(juegos[1].trim());

                if ((j1 >= 6 && (j1 - j2 >= 2)) || (j1 == 7 && (j2 == 5 || j2 == 6))) {
                    setsJ1++;
                } else if ((j2 >= 6 && (j2 - j1 >= 2)) || (j2 == 7 && (j1 == 5 || j1 == 6))) {
                    setsJ2++;
                }

            } catch (NumberFormatException e) {
                // 🔥 ACÁ CAMBIA TODO
                throw new IllegalArgumentException("Score inválido: " + set, e);
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

    private void validarCantidadSets(Partido partido, String[] sets, String scoreOriginal) {

        String scoreUpper = scoreOriginal.toUpperCase();

        // 🚨 SALTEAMOS VALIDACIÓN PARA CASOS ESPECIALES
        if (scoreUpper.contains("RET") || scoreUpper.contains("DEF") || scoreUpper.contains("SUSP")) {
            return;
        }

        boolean esGrandSlam = partido.getCategoria().getPuntos() >= 2000;

        if (esGrandSlam && sets.length < 3) {
            throw new IllegalArgumentException("Un Grand Slam debe tener al menos 3 sets (formato al mejor de 5)");
        }

        if (!esGrandSlam && sets.length > 3) {
            throw new IllegalArgumentException("Un torneo ATP no puede tener más de 3 sets (formato al mejor de 3)");
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

    private void sincronizarSalesforce(Partido partido) {
        if (partido == null) return;

        if (partido.getEstado() == EstadoPartido.FINALIZADO ||
                partido.getEstado() == EstadoPartido.RETIRO ||
                partido.getEstado() == EstadoPartido.DESCALIFICACION) {

            try {
                salesforceService.enviarPartidoASalesforce(partido);
                System.out.println("✅ [SALESFORCE] Sincronización exitosa del partido ID: " + partido.getId());
            } catch (Exception e) {
                System.err.println("=========================================");
                System.err.println("❌ [SALESFORCE] El envío REBOTÓ para el partido ID: " + partido.getId());
                System.err.println("MOTIVO: " + e.getMessage());
                System.err.println("=========================================");
            }
        }
    }
}