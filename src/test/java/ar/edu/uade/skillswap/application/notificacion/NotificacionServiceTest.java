package ar.edu.uade.skillswap.application.notificacion;

import ar.edu.uade.skillswap.application.notificacion.dto.NotificacionDTO;
import ar.edu.uade.skillswap.application.shared.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.contratacion.evento.ContratacionAceptada;
import ar.edu.uade.skillswap.domain.contratacion.evento.SesionCompletada;
import ar.edu.uade.skillswap.domain.notificacion.Notificacion;
import ar.edu.uade.skillswap.domain.notificacion.exception.NotificacionAjenaException;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.infrastructure.memoria.NotificacionRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.UnidadDeTrabajoEnMemoria;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificacionServiceTest {

    private static final String MATEO = "user-mateo";
    private static final String ANA = "user-ana";
    private static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");

    private final UnidadDeTrabajoEnMemoria unidadDeTrabajo = new UnidadDeTrabajoEnMemoria();
    private final NotificacionRepositoryEnMemoria notificaciones = new NotificacionRepositoryEnMemoria(unidadDeTrabajo);
    private final NotificacionService service = new NotificacionService(notificaciones, unidadDeTrabajo);
    private Notificacion aceptada;

    @BeforeEach
    void llenarBandejas() {
        var contratacion = ContratacionId.generate();
        aceptada = Notificacion.para(MATEO, new ContratacionAceptada(contratacion, MATEO, ANA, AHORA));
        notificaciones.save(aceptada);
        var completada = new SesionCompletada(contratacion, MATEO, ANA, AHORA.plusSeconds(60));
        notificaciones.save(Notificacion.para(MATEO, completada));
        notificaciones.save(Notificacion.para(ANA, completada));
    }

    private List<NotificacionDTO> bandeja(String userId) {
        return service.listar(userId, PaginaSolicitada.primera(10)).elementos();
    }

    @Test
    void listaLaBandejaDelUsuarioDeLaMasNuevaALaMasVieja() {
        var pagina = service.listar(MATEO, PaginaSolicitada.primera(1));

        assertEquals(2, pagina.total());
        assertEquals("SESION_COMPLETADA", pagina.elementos().get(0).tipo());
        assertFalse(pagina.elementos().get(0).leida());
        assertEquals(1, bandeja(ANA).size());
    }

    @Test
    void elDestinatarioMarcaUnaComoLeida() {
        service.marcarLeida(MATEO, aceptada.id().valor().toString());

        var leidas = bandeja(MATEO).stream().filter(NotificacionDTO::leida).map(NotificacionDTO::tipo).toList();
        assertEquals(List.of("CONTRATACION_ACEPTADA"), leidas);
    }

    @Test
    void nadieMarcaUnaNotificacionAjena() {
        assertThrows(NotificacionAjenaException.class,
                () -> service.marcarLeida(ANA, aceptada.id().valor().toString()));
        assertTrue(bandeja(MATEO).stream().noneMatch(NotificacionDTO::leida));
    }

    @Test
    void unaNotificacionInexistenteSeInformaComoNoEncontrada() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.marcarLeida(MATEO, "no-es-un-id"));
    }

    @Test
    void marcarTodasSoloTocaLasDelUsuario() {
        service.marcarTodasLeidas(MATEO);

        assertTrue(bandeja(MATEO).stream().allMatch(NotificacionDTO::leida));
        assertTrue(bandeja(ANA).stream().noneMatch(NotificacionDTO::leida));
        assertTrue(notificaciones.findNoLeidas(MATEO).isEmpty());
    }
}
