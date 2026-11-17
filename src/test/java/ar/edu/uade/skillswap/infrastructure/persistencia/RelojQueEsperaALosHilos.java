package ar.edu.uade.skillswap.infrastructure.persistencia;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reloj fijo para los tests de concurrencia. Después de {@link #esperarA}, la primera vez que cada hilo
 * pide la hora se queda esperando hasta que todos los hilos la pidieron. Como {@code aceptar} pide la
 * hora después de leer la contratación y la publicación, esto garantiza que las dos transacciones leen
 * el mismo cupo antes de que alguna guarde: la carrera ocurre siempre, no por casualidad.
 */
class RelojQueEsperaALosHilos extends Clock {

    private final Instant ahora;
    private volatile CountDownLatch hilosQueFaltan = new CountDownLatch(0);
    private final AtomicReference<Retencion> retencion = new AtomicReference<>();

    /** El próximo hilo que pida la hora avisa con {@code llego} y se queda esperando hasta {@code soltar}. */
    private record Retencion(CountDownLatch llego, CountDownLatch soltar) {
    }

    RelojQueEsperaALosHilos(Instant ahora) {
        this.ahora = ahora;
    }

    void esperarA(int hilos) {
        hilosQueFaltan = new CountDownLatch(hilos);
    }

    /**
     * El próximo hilo que pida la hora queda retenido: cuenta {@code llego} y espera a {@code soltar}. Sirve
     * para forzar un orden entre dos operaciones: una lee y queda retenida antes de guardar mientras la otra
     * termina entera.
     */
    void retenerAlProximoHilo(CountDownLatch llego, CountDownLatch soltar) {
        retencion.set(new Retencion(llego, soltar));
    }

    @Override
    public Instant instant() {
        var retenido = retencion.getAndSet(null);
        if (retenido != null) {
            retenido.llego().countDown();
            esperar(retenido.soltar());
            return ahora;
        }
        var latch = hilosQueFaltan;
        latch.countDown();
        esperar(latch);
        return ahora;
    }

    private static void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Los hilos no llegaron a tiempo");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
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
