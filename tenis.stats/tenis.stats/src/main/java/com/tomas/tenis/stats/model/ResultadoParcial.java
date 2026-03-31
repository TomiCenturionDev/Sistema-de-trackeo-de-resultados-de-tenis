package com.tomas.tenis.stats.model;

public record ResultadoParcial(int juegosJ1, int juegosJ2) {

    @Override
    public String toString() {
        return juegosJ1 + "-" + juegosJ2;
    }
}