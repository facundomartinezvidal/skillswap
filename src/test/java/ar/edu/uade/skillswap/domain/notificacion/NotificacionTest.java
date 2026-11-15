package ar.edu.uade.skillswap.domain.notificacion;

import ar.edu.uade.skillswap.domain.notificacion.exception.NotificacionAjenaException;
import ar.edu.uade.skillswap.domain.shared.EventoDominio;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Usa un evento propio del test: la notificación no conoce los eventos de contratación. */
class NotificacionTest {

    private static final String DESTINATARIO = "user-ana";
    private static final String OTRO_INTERESADO = "user-mateo";
    private static final String AJENO = "user-otro";
    private static final Instant FECHA = Instant.parse("2026-11-11T12:00:00Z");

    private record EventoDePrueba(List<String> interesados) implements EventoDominio {

        @Override
        public String tipo() {
            return "EVENTO_DE_PRUEBA";
        }

        @Override
        public String titulo() {
            return "Título de prueba";
        }

        @Override
        public String detalle() {
            return "Detalle de prueba";
        }

        @Override
        public Instant ocurridoEn() {
            return FECHA;
        }
    }

    private static final EventoDominio EVENTO = new EventoDePrueba(List.of(DESTINATARIO, OTRO_INTERESADO));

    @Test
    void copiaLosDatosDelEventoYEmpiezaSinLeer() {
        var notificacion = Notificacion.para(DESTINATARIO, EVENTO);

        assertNotNull(notificacion.id());
        assertEquals(DESTINATARIO, notificacion.destinatarioId());
        assertEquals("EVENTO_DE_PRUEBA", notificacion.tipo());
        assertEquals("Título de prueba", notificacion.titulo());
        assertEquals("Detalle de prueba", notificacion.detalle());
        assertEquals(FECHA, notificacion.fecha());
        assertFalse(notificacion.estaLeida());
    }

    @Test
    void cadaInteresadoRecibeSuPropiaNotificacion() {
        var paraUno = Notificacion.para(DESTINATARIO, EVENTO);
        var paraOtro = Notificacion.para(OTRO_INTERESADO, EVENTO);

        assertNotEquals(paraUno.id(), paraOtro.id());
        assertEquals(OTRO_INTERESADO, paraOtro.destinatarioId());
    }

    @Test
    void rechazaUnDestinatarioQueNoEsInteresadoEnElEvento() {
        assertThrows(IllegalArgumentException.class, () -> Notificacion.para(AJENO, EVENTO));
    }

    @Test
    void rechazaDatosFaltantes() {
        assertThrows(IllegalArgumentException.class, () -> Notificacion.para(null, EVENTO));
        assertThrows(IllegalArgumentException.class, () -> Notificacion.para(" ", EVENTO));
        assertThrows(NullPointerException.class, () -> Notificacion.para(DESTINATARIO, null));
    }

    @Test
    void elDestinatarioLaMarcaComoLeida() {
        var notificacion = Notificacion.para(DESTINATARIO, EVENTO);

        notificacion.marcarLeida(DESTINATARIO);

        assertTrue(notificacion.estaLeida());
    }

    @Test
    void marcarlaDosVecesNoFallaYSigueLeida() {
        var notificacion = Notificacion.para(DESTINATARIO, EVENTO);
        notificacion.marcarLeida(DESTINATARIO);

        assertDoesNotThrow(() -> notificacion.marcarLeida(DESTINATARIO));

        assertTrue(notificacion.estaLeida());
    }

    @Test
    void otroUsuarioNoPuedeMarcarlaComoLeida() {
        var notificacion = Notificacion.para(DESTINATARIO, EVENTO);

        for (var actor : new String[]{OTRO_INTERESADO, AJENO, null}) {
            var ex = assertThrows(NotificacionAjenaException.class, () -> notificacion.marcarLeida(actor));
            assertEquals(actor, ex.actorId());
            assertEquals(notificacion.id(), ex.notificacionId());
        }
        assertFalse(notificacion.estaLeida());
    }
}
