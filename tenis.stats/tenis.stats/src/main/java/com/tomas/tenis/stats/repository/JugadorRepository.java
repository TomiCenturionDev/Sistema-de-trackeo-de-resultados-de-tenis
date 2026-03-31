package com.tomas.tenis.stats.repository;

import com.tomas.tenis.stats.model.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

    @Repository
    public interface JugadorRepository extends JpaRepository<Jugador, Long> {

    }
