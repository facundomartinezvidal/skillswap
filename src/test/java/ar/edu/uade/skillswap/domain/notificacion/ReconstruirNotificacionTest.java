package ar.edu.uade.skillswap.domain.notificacion;

import ar.edu.uade.skillswap.domain.notificacion.exception.NotificacionAjenaException;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ReconstruirNotificacionTest {

    private static final Instant FECHA = Instant.parse("2026-11-15T12:00:00Z");

    @Test
    void conservaLosDatosYSiEstabaLeida() {
        var id = NotificacionId.generate();

        var notificacion = Notificacion.reconstruir(id, "user-ana", "PAGO_CONFIRMADO", "Pago confirmado",
                "El cliente pagó la contratación.", FECHA, true);

        assertEquals(id, notificacion.id());
        assertEquals("user-ana", notificacion.destinatarioId());
        assertEquals("PAGO_CONFIRMADO", notificacion.tipo());
        assertEquals(FECHA, notificacion.fecha());
        assertTrue(notificacion.estaLeida());
    }

    @Test
    void soloElDestinatarioPuedeMarcarlaComoLeida() {
        var notificacion = Notificacion.reconstruir(NotificacionId.generate(), "user-ana", "PAGO_CONFIRMADO",
                "Pago confirmado", "El cliente pagó la contratación.", FECHA, false);

        assertThrows(NotificacionAjenaException.class, () -> notificacion.marcarLeida("user-mateo"));
        notificacion.marcarLeida("user-ana");
        assertTrue(notificacion.estaLeida());
    }
}
