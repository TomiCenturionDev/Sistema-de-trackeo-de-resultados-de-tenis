package com.tomas.tenis.stats.repository;
import com.tomas.tenis.stats.model.FaseTorneo;
import com.tomas.tenis.stats.model.Partido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PartidoRepository extends JpaRepository<Partido, Long> {
    // En PartidoRepository.java
    Optional<Partido> findByTorneoAndFechaAndFaseAndJugador1IdAndJugador2Id(
            String torneo,
            LocalDate fecha,
            FaseTorneo fase,
            Long jugador1Id,
            Long jugador2Id
    );
}

