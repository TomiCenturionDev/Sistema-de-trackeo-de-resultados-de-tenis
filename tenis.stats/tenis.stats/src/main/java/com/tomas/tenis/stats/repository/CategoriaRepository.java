package com.tomas.tenis.stats.repository;

import com.tomas.tenis.stats.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    // Al extender de JpaRepository, el método findById ya viene incluido por defecto.
}