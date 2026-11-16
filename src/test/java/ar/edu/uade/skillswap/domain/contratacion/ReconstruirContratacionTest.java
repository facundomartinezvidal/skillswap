package ar.edu.uade.skillswap.domain.contratacion;

import ar.edu.uade.skillswap.domain.publicacion.FranjaId;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.shared.TransicionInvalidaException;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ReconstruirContratacionTest {

    private static final String CLIENTE = "user-mateo";
    private static final String PROVEEDOR = "user-ana";
    private static final Instant AHORA = Instant.parse("2026-11-15T12:00:00Z");
    private static final Instant INICIO = Instant.parse("2026-11-20T10:00:00Z");
    private static final Instant OTRO_INICIO = Instant.parse("2026-11-21T10:00:00Z");
    private static final FranjaId FRANJA = FranjaId.generate();

    private static Contratacion aceptada() {
        var contratacion = Contratacion.solicitar(CLIENTE, PROVEEDOR, PublicacionId.generate(),
                new Solicitud("Hola", "Mejorar mis tests", FRANJA), INICIO, new SkillCoins(100), AHORA);
        contratacion.aceptar(PROVEEDOR, AHORA);
        return contratacion;
    }

    /** Lo mismo que hace un repositorio: armarla de nuevo a partir de lo que guardó. */
    private static Contratacion copiaDe(Contratacion c) {
        return Contratacion.reconstruir(c.id(), c.publicacionId(), c.clienteId(), c.proveedorId(), c.solicitud(),
                c.franjaId(), c.inicioSesion(), c.precioAcordado(), c.estado(), c.propuesta(), c.reintegro(),
                c.historial(), c.calificaciones());
    }

    @Test
    void conservaElEstadoYElHistorialSinEventosPendientes() {
        var original = aceptada();
        original.pagar(CLIENTE, AHORA);

        var copia = copiaDe(original);

        assertEquals(original.id(), copia.id());
        assertEquals("CONFIRMADA", copia.estado().nombre());
        assertEquals(original.historial(), copia.historial());
        assertEquals(FRANJA, copia.franjaId());
        assertTrue(copia.extraerEventos().isEmpty());
    }

    @Test
    void sigueAplicandoLasReglasDeSuEstado() {
        var original = aceptada();
        original.pagar(CLIENTE, AHORA);
        var copia = copiaDe(original);

        assertThrows(TransicionInvalidaException.class, () -> copia.pagar(CLIENTE, AHORA));
        copia.iniciar(PROVEEDOR, AHORA);
        assertEquals("EN_CURSO", copia.estado().nombre());
    }

    @Test
    void cambiarLaCopiaNoCambiaLaOriginal() {
        var original = aceptada();
        var copia = copiaDe(original);

        copia.pagar(CLIENTE, AHORA);

        assertEquals("ACEPTADA", original.estado().nombre());
        assertEquals(1, original.historial().size());
    }

    @Test
    void conservaLaPropuestaDeReprogramacionPendiente() {
        var original = aceptada();
        var otraFranja = FranjaId.generate();
        original.proponerReprogramacion(CLIENTE, otraFranja, OTRO_INICIO, AHORA);

        var copia = copiaDe(original);
        var anterior = copia.aceptarReprogramacion(PROVEEDOR, AHORA);

        assertEquals(FRANJA, anterior);
        assertEquals(otraFranja, copia.franjaId());
        assertEquals(OTRO_INICIO, copia.inicioSesion());
    }
}
