package ar.edu.uade.skillswap.controllers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj fijo que el test puede adelantar, para que dos operaciones no ocurran en el mismo instante. */
final class RelojDePrueba extends Clock {

    private Instant ahora;

    RelojDePrueba(Instant inicio) {
        this.ahora = inicio;
    }

    void adelantar(Duration duracion) {
        ahora = ahora.plus(duracion);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(ahora, zone);
    }

    @Override
    public Instant instant() {
        return ahora;
    }
}
