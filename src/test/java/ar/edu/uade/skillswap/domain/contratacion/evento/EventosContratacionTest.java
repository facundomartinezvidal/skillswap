package ar.edu.uade.skillswap.domain.contratacion.evento;

import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.shared.EventoDominio;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventosContratacionTest {

    private static final ContratacionId ID = ContratacionId.generate();
    private static final String CLIENTE = "user-mateo";
    private static final String PROVEEDOR = "user-ana";
    private static final Instant AHORA = Instant.parse("2026-11-11T12:00:00Z");
    private static final Instant NUEVO_INICIO = Instant.parse("2026-11-21T10:00:00Z");

    @Test
    void cadaEventoDeclaraSusInteresados() {
        assertEquals(List.of(PROVEEDOR), new SolicitudRecibida(ID, CLIENTE, PROVEEDOR, AHORA).interesados());
        assertEquals(List.of(CLIENTE), new ContratacionAceptada(ID, CLIENTE, PROVEEDOR, AHORA).interesados());
        assertEquals(List.of(PROVEEDOR), new PagoConfirmado(ID, CLIENTE, PROVEEDOR, AHORA).interesados());
        assertEquals(List.of(PROVEEDOR),
                new ReprogramacionPropuesta(ID, CLIENTE, PROVEEDOR, NUEVO_INICIO, AHORA).interesados());
        assertEquals(List.of(CLIENTE),
                new ReprogramacionAceptada(ID, CLIENTE, PROVEEDOR, NUEVO_INICIO, AHORA).interesados());
        assertEquals(List.of(CLIENTE),
                new ContratacionCancelada(ID, PROVEEDOR, CLIENTE, "me enfermé", AHORA).interesados());
        assertEquals(List.of(CLIENTE, PROVEEDOR), new SesionCompletada(ID, CLIENTE, PROVEEDOR, AHORA).interesados());
    }

    @Test
    void cadaEventoTieneUnTipoEstableYDistintoYTextoParaElUsuario() {
        var eventos = todos();
        var tipos = new HashSet<String>();

        for (var evento : eventos) {
            assertTrue(tipos.add(evento.tipo()), "Tipo repetido: " + evento.tipo());
            assertFalse(evento.titulo().isBlank(), evento.tipo());
            assertFalse(evento.detalle().isBlank(), evento.tipo());
            assertEquals(AHORA, evento.ocurridoEn(), evento.tipo());
        }
        assertEquals(List.of("SOLICITUD_RECIBIDA", "CONTRATACION_ACEPTADA", "PAGO_CONFIRMADO",
                        "REPROGRAMACION_PROPUESTA", "REPROGRAMACION_ACEPTADA", "CONTRATACION_CANCELADA",
                        "SESION_COMPLETADA"),
                eventos.stream().map(EventoDominio::tipo).toList());
    }

    @Test
    void laCancelacionIncluyeElMotivoYLaReprogramacionElNuevoInicio() {
        assertTrue(new ContratacionCancelada(ID, CLIENTE, PROVEEDOR, "cambié de planes", AHORA)
                .detalle().contains("cambié de planes"));
        assertTrue(new ReprogramacionPropuesta(ID, CLIENTE, PROVEEDOR, NUEVO_INICIO, AHORA)
                .detalle().contains(NUEVO_INICIO.toString()));
        assertTrue(new ReprogramacionAceptada(ID, CLIENTE, PROVEEDOR, NUEVO_INICIO, AHORA)
                .detalle().contains(NUEVO_INICIO.toString()));
    }

    @Test
    void losInteresadosNoSePuedenModificar() {
        var interesados = new SesionCompletada(ID, CLIENTE, PROVEEDOR, AHORA).interesados();

        assertThrows(UnsupportedOperationException.class, () -> interesados.add("user-otro"));
    }

    @Test
    void rechazanDatosFaltantes() {
        assertThrows(NullPointerException.class, () -> new SolicitudRecibida(null, CLIENTE, PROVEEDOR, AHORA));
        assertThrows(NullPointerException.class, () -> new PagoConfirmado(ID, CLIENTE, PROVEEDOR, null));
        assertThrows(NullPointerException.class,
                () -> new ContratacionCancelada(ID, CLIENTE, PROVEEDOR, null, AHORA));
        assertThrows(NullPointerException.class,
                () -> new ReprogramacionPropuesta(ID, CLIENTE, PROVEEDOR, null, AHORA));
    }

    private static List<EventoDominio> todos() {
        return List.of(
                new SolicitudRecibida(ID, CLIENTE, PROVEEDOR, AHORA),
                new ContratacionAceptada(ID, CLIENTE, PROVEEDOR, AHORA),
                new PagoConfirmado(ID, CLIENTE, PROVEEDOR, AHORA),
                new ReprogramacionPropuesta(ID, CLIENTE, PROVEEDOR, NUEVO_INICIO, AHORA),
                new ReprogramacionAceptada(ID, CLIENTE, PROVEEDOR, NUEVO_INICIO, AHORA),
                new ContratacionCancelada(ID, CLIENTE, PROVEEDOR, "motivo", AHORA),
                new SesionCompletada(ID, CLIENTE, PROVEEDOR, AHORA));
    }
}
