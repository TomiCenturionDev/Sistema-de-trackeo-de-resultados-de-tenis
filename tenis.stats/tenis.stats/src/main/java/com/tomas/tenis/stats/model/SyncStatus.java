package com.tomas.tenis.stats.model;

public enum SyncStatus {
        PENDING,
        SUCCESS,
        FAILED,        // se puede reintentar
        FAILED_FINAL   // no se reintenta más
}
