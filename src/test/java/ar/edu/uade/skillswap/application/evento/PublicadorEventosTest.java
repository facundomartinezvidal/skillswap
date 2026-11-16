package ar.edu.uade.skillswap.application.evento;

import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.contratacion.evento.PagoConfirmado;
import ar.edu.uade.skillswap.domain.contratacion.evento.SolicitudRecibida;
import ar.edu.uade.skillswap.domain.shared.EventoDominio;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PublicadorEventosTest {

    private static final Instant AHORA = Instant.parse("2026-11-15T12:00:00Z");

    private final PublicadorEventos publicador = new PublicadorEventos();
    private final ContratacionId contratacion = ContratacionId.generate();
    private final EventoDominio solicitud = new SolicitudRecibida(contratacion, "user-mateo", "user-ana", AHORA);
    private final EventoDominio pago = new PagoConfirmado(contratacion, "user-mateo", "user-ana", AHORA);

    /** Suscriptor de prueba que anota lo que recibe en una lista compartida. */
    private record Anotador(String nombre, List<String> recibidos) implements SuscriptorEvento {
        @Override
        public void actualizar(EventoDominio evento) {
            recibidos.add(nombre + ":" + evento.tipo());
        }
    }

    @Test
    void cadaSuscriptorRecibeCadaEventoEnOrden() {
        var recibidos = new ArrayList<String>();
        publicador.suscribir(new Anotador("bandeja", recibidos));
        publicador.suscribir(new Anotador("auditoria", recibidos));

        publicador.publicar(List.of(solicitud, pago));

        assertEquals(List.of(
                "bandeja:SOLICITUD_RECIBIDA", "auditoria:SOLICITUD_RECIBIDA",
                "bandeja:PAGO_CONFIRMADO", "auditoria:PAGO_CONFIRMADO"), recibidos);
    }

    @Test
    void unSuscriptorQueFallaNoCortaALosDemas() {
        var recibidos = new ArrayList<String>();
        publicador.suscribir(evento -> {
            throw new IllegalStateException("el servidor de email no responde");
        });
        publicador.suscribir(new Anotador("bandeja", recibidos));

        assertDoesNotThrow(() -> publicador.publicar(List.of(solicitud, pago)));

        assertEquals(List.of("bandeja:SOLICITUD_RECIBIDA", "bandeja:PAGO_CONFIRMADO"), recibidos);
    }

    @Test
    void dosSuscriptoresIgualesPeroDistintosRecibenLosDos() {
        var recibidos = new ArrayList<String>();
        publicador.suscribir(new Anotador("bandeja", recibidos));
        publicador.suscribir(new Anotador("bandeja", recibidos));

        publicador.publicar(List.of(pago));

        assertEquals(List.of("bandeja:PAGO_CONFIRMADO", "bandeja:PAGO_CONFIRMADO"), recibidos);
    }

    @Test
    void suscribirDosVecesAlMismoNoDuplicaLosAvisos() {
        var recibidos = new ArrayList<String>();
        var bandeja = new Anotador("bandeja", recibidos);
        publicador.suscribir(bandeja);
        publicador.suscribir(bandeja);

        publicador.publicar(List.of(pago));

        assertEquals(List.of("bandeja:PAGO_CONFIRMADO"), recibidos);
    }

    @Test
    void sinSuscriptoresOSinEventosNoPasaNada() {
        assertDoesNotThrow(() -> publicador.publicar(List.of(pago)));

        var recibidos = new ArrayList<String>();
        publicador.suscribir(new Anotador("bandeja", recibidos));
        publicador.publicar(List.of());

        assertTrue(recibidos.isEmpty());
    }

    @Test
    void rechazaSuscriptorOEventosNulos() {
        assertThrows(NullPointerException.class, () -> publicador.suscribir(null));
        assertThrows(NullPointerException.class, () -> publicador.publicar(null));
    }
}
