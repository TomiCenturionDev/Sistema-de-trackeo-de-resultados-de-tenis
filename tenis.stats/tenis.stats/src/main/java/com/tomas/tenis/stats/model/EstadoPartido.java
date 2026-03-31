package com.tomas.tenis.stats.model;

public enum EstadoPartido {
    PROGRAMADO,      // Todavía no empezó (0-0)
    EN_CURSO,        // Se están jugando sets
    FINALIZADO,      // Terminado por score (según la clase Resultado)
    SUSPENDIDO,      // Por lluvia, falta de luz o lesión momentánea
    RETIRO,          // Abandono por lesión (el famoso RET)
    DESCALIFICACION  // Expulsión por conducta (como Rublev o Shapovalov)
}