package ar.edu.uade.skillswap;

import ar.edu.uade.skillswap.application.busqueda.CriteriosBusqueda;
import ar.edu.uade.skillswap.application.busqueda.OrdenBusqueda;
import ar.edu.uade.skillswap.application.busqueda.recomendacion.AfinidadPorHabilidades;
import ar.edu.uade.skillswap.application.contratacion.dto.SolicitarContratacionCommand;
import ar.edu.uade.skillswap.application.notificacion.dto.NotificacionDTO;
import ar.edu.uade.skillswap.application.perfil.dto.CrearPerfilCommand;
import ar.edu.uade.skillswap.application.perfil.dto.EditarPerfilCommand;
import ar.edu.uade.skillswap.application.publicacion.dto.DatosPublicacionCommand;
import ar.edu.uade.skillswap.application.publicacion.dto.PromocionDTO;
import ar.edu.uade.skillswap.application.publicacion.dto.PublicacionDTO;
import ar.edu.uade.skillswap.domain.perfil.CanalNotificacion;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.domain.publicacion.Modalidad;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.infrastructure.configuracion.BackendEnMemoria;
import ar.edu.uade.skillswap.infrastructure.identidad.ServicioIdentidadSimulado;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El recorrido de la demo de punta a punta, con todo el backend armado por {@link BackendEnMemoria}
 * (los mismos Services que van a usar los controllers): publicar → buscar → solicitar → aceptar →
 * pagar → completar → calificar, más cancelar con reintegro y las notificaciones por canal.
 */
class RecorridoCompletoTest {

    private static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");
    private static final String ANA = "user-ana";
    private static final String MATEO = "user-mateo";
    private static final long BONO = BackendEnMemoria.BONO_DEMO.valor();

    private final BackendEnMemoria backend = new BackendEnMemoria(Clock.fixed(AHORA, ZoneOffset.UTC),
            ServicioIdentidadSimulado.conUsuariosDeDemo(), BackendEnMemoria.BONO_DEMO, AfinidadPorHabilidades::new);
    private PublicacionDTO mentoria;
    private String franjaEn72Horas;
    private String franjaEn30Horas;

    @BeforeEach
    void anaPublicaUnaMentoria() {
        backend.perfilService.crearPerfil(new CrearPerfilCommand(ANA, TipoPerfil.PROFESIONAL, "Ana", ZoneId.of("UTC")));
        backend.perfilService.crearPerfil(new CrearPerfilCommand(MATEO, TipoPerfil.PROFESIONAL, "Mateo", ZoneId.of("UTC")));
        backend.perfilService.editarPerfil(ANA, new EditarPerfilCommand("Ana", "Mentora de Java", ZoneId.of("UTC"),
                Set.of("Java"), null));
        backend.perfilService.elegirCanal(ANA, CanalNotificacion.PUSH);
        backend.perfilService.elegirCanal(MATEO, CanalNotificacion.EMAIL);

        var borrador = backend.publicacionService.crearBorrador(ANA, new DatosPublicacionCommand("Mentoría de Java",
                "Revisión de código y buenas prácticas", "Programación", Modalidad.REMOTA, Duration.ofHours(1), 2,
                100L, Set.of("java", "backend")));
        backend.publicacionService.agregarFranja(ANA, borrador.id(), AHORA.plus(Duration.ofHours(72)),
                AHORA.plus(Duration.ofHours(73)));
        backend.publicacionService.agregarFranja(ANA, borrador.id(), AHORA.plus(Duration.ofHours(30)),
                AHORA.plus(Duration.ofHours(31)));
        mentoria = backend.publicacionService.publicar(ANA, borrador.id());
        franjaEn72Horas = mentoria.franjas().stream().filter(f -> f.inicio().equals(AHORA.plus(Duration.ofHours(72))))
                .findFirst().orElseThrow().id();
        franjaEn30Horas = mentoria.franjas().stream().filter(f -> f.inicio().equals(AHORA.plus(Duration.ofHours(30))))
                .findFirst().orElseThrow().id();
    }

    private String solicitar(String franjaId) {
        return backend.contratacionService.solicitar(new SolicitarContratacionCommand(MATEO, mentoria.id(), franjaId,
                "Quiero mejorar mi proyecto", "Aprender a testear")).id();
    }

    private List<String> bandeja(String userId) {
        return backend.notificacionService.listar(userId, PaginaSolicitada.primera(20)).elementos().stream()
                .map(NotificacionDTO::tipo).toList();
    }

    @Test
    void publicarBuscarSolicitarAceptarPagarCompletarYCalificar() {
        var encontradas = backend.busquedaService.buscar(new CriteriosBusqueda("java", "Programación",
                Modalidad.REMOTA, null, 150L, null, null, OrdenBusqueda.RELEVANCIA), PaginaSolicitada.primera(10));
        assertEquals(List.of(mentoria.id()), encontradas.elementos().stream().map(PublicacionDTO::id).toList());

        var id = solicitar(franjaEn72Horas);
        backend.contratacionService.aceptar(ANA, id);
        backend.contratacionService.pagar(MATEO, id);
        assertEquals(BONO - 100, backend.walletService.consultarSaldo(MATEO));
        backend.contratacionService.iniciar(ANA, id);
        var completada = backend.contratacionService.completar(ANA, id);
        backend.contratacionService.calificar(MATEO, id, 5, "Excelente mentora");
        backend.contratacionService.calificar(ANA, id, 4, null);

        assertEquals("COMPLETADA", completada.estado());
        assertEquals(BONO + 100, backend.walletService.consultarSaldo(ANA));
        assertEquals(BONO - 100, backend.walletService.consultarSaldo(MATEO));
        var perfilAna = backend.perfilService.verPerfil(ANA);
        assertEquals(5.0, perfilAna.reputacion());
        assertEquals(1, perfilAna.contratacionesCompletadas());
        assertEquals(List.of(mentoria.id()), perfilAna.publicacionesActivas().stream().map(PublicacionDTO::id).toList());
        assertEquals(List.of("SESION_COMPLETADA", "CONTRATACION_ACEPTADA"), bandeja(MATEO));
        assertEquals(List.of("SESION_COMPLETADA", "PAGO_CONFIRMADO", "SOLICITUD_RECIBIDA"), bandeja(ANA));
        assertEquals(2, backend.email.enviados().size(), "Mateo eligió email");
        assertEquals(3, backend.push.enviados().size(), "Ana eligió push");
        assertEquals(2, backend.contratacionService.listarContrataciones(MATEO, PaginaSolicitada.primera(10))
                .elementos().get(0).calificaciones().size());
    }

    @Test
    void cancelarConReintegroSegunLaAnticipacion() {
        var id = solicitar(franjaEn30Horas);
        backend.contratacionService.aceptar(ANA, id);
        backend.contratacionService.pagar(MATEO, id);

        var cancelada = backend.contratacionService.cancelar(MATEO, id, "Me surgió un viaje");

        assertEquals("CANCELADA", cancelada.estado());
        assertEquals(50, cancelada.reintegro().alCliente(), "con 30 h de anticipación vuelve el 50 %");
        assertEquals(BONO - 50, backend.walletService.consultarSaldo(MATEO));
        assertEquals(BONO + 50, backend.walletService.consultarSaldo(ANA));
        assertEquals(2, backend.publicacionService.verPublicacion(MATEO, mentoria.id()).franjas().stream()
                .filter(f -> f.id().equals(franjaEn30Horas)).findFirst().orElseThrow().lugaresLibres());
        assertEquals("CONTRATACION_CANCELADA", bandeja(ANA).get(0));
    }

    @Test
    void lasPromocionesSeAplicanAlPrecioQueSePaga() {
        backend.publicacionService.agregarPromocion(ANA, mentoria.id(), new PromocionDTO(PromocionDTO.Tipo.PORCENTUAL, 20));

        var id = solicitar(franjaEn72Horas);
        backend.contratacionService.aceptar(ANA, id);
        backend.contratacionService.pagar(MATEO, id);

        assertEquals(BONO - 80, backend.walletService.consultarSaldo(MATEO));
        var movimientos = backend.walletService.listarMovimientos(MATEO, PaginaSolicitada.primera(10));
        assertEquals(-80, movimientos.elementos().get(0).importe());
    }

    @Test
    void elBackendDeDemoArrancaConLaIdentidadSimulada() {
        var demo = new BackendEnMemoria();

        var perfil = demo.perfilService.crearPerfil(new CrearPerfilCommand(MATEO, TipoPerfil.PROFESIONAL, "Mateo",
                ZoneId.of("UTC")));

        assertEquals(MATEO, perfil.userId());
        assertEquals(BONO, demo.walletService.consultarSaldo(MATEO));
    }
}
