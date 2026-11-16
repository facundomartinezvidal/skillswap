package ar.edu.uade.skillswap.infrastructure.persistencia;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Reloj fijo para los tests de concurrencia. Después de {@link #esperarA}, la primera vez que cada hilo
 * pide la hora se queda esperando hasta que todos los hilos la pidieron. Como {@code aceptar} pide la
 * hora después de leer la contratación y la publicación, esto garantiza que las dos transacciones leen
 * el mismo cupo antes de que alguna guarde: la carrera ocurre siempre, no por casualidad.
 */
class RelojQueEsperaALosHilos extends Clock {

    private final Instant ahora;
    private volatile CountDownLatch hilosQueFaltan = new CountDownLatch(0);

    RelojQueEsperaALosHilos(Instant ahora) {
        this.ahora = ahora;
    }

    void esperarA(int hilos) {
        hilosQueFaltan = new CountDownLatch(hilos);
    }

    @Override
    public Instant instant() {
        var latch = hilosQueFaltan;
        latch.countDown();
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Los hilos no llegaron a tiempo");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return ahora;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
