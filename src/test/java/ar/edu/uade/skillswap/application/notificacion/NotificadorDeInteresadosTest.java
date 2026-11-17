package ar.edu.uade.skillswap.application.notificacion;

import ar.edu.uade.skillswap.application.contratacion.ContratacionService;
import ar.edu.uade.skillswap.application.contratacion.dto.SolicitarContratacionCommand;
import ar.edu.uade.skillswap.application.evento.PublicadorEventos;
import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.contratacion.evento.PagoConfirmado;
import ar.edu.uade.skillswap.domain.contratacion.evento.SesionCompletada;
import ar.edu.uade.skillswap.domain.contratacion.strategy.PoliticaReintegroFactory;
import ar.edu.uade.skillswap.domain.contratacion.strategy.ReintegroPorAnticipacion;
import ar.edu.uade.skillswap.domain.perfil.CanalNotificacion;
import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.domain.publicacion.Modalidad;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.publicacion.TipoPublicacion;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.wallet.RetencionDePagos;
import ar.edu.uade.skillswap.domain.wallet.Wallet;
import ar.edu.uade.skillswap.infrastructure.memoria.ContratacionRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.NotificacionRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.PerfilRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.PublicacionRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.UnidadDeTrabajoEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.WalletRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.notificacion.EmailAdapter;
import ar.edu.uade.skillswap.infrastructure.notificacion.ProveedorEmailSimulado;
import ar.edu.uade.skillswap.infrastructure.notificacion.ProveedorPushSimulado;
import ar.edu.uade.skillswap.infrastructure.notificacion.PushAdapter;

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
 * Observer + Simple Factory + Adapter. Cubre el caso de aceptación 8: un mismo evento (sesión
 * completada) le interesa al cliente y al proveedor, y cada uno lo recibe por su canal (email y push)
 * mediante adaptadores simulados. Además, todos lo tienen en su bandeja.
 */
class NotificadorDeInteresadosTest {

    private static final String CLIENTE = "user-mateo";
    private static final String PROVEEDOR = "user-ana";
    private static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");

    private final UnidadDeTrabajoEnMemoria unidadDeTrabajo = new UnidadDeTrabajoEnMemoria();
    private final PerfilRepositoryEnMemoria perfiles = new PerfilRepositoryEnMemoria(unidadDeTrabajo);
    private final NotificacionRepositoryEnMemoria notificaciones = new NotificacionRepositoryEnMemoria(unidadDeTrabajo);
    private final ProveedorEmailSimulado email = new ProveedorEmailSimulado();
    private final ProveedorPushSimulado push = new ProveedorPushSimulado();
    private final NotificadorDeInteresados notificador = new NotificadorDeInteresados(notificaciones, perfiles,
            new CanalEnvioFactory(new EmailAdapter(email), new PushAdapter(push)));

    @BeforeEach
    void crearPerfiles() {
        guardarPerfil(CLIENTE, CanalNotificacion.EMAIL);
        guardarPerfil(PROVEEDOR, CanalNotificacion.PUSH);
    }

    private void guardarPerfil(String userId, CanalNotificacion canal) {
        var perfil = Perfil.crear(userId, TipoPerfil.PROFESIONAL, userId, ZoneId.of("UTC"));
        perfil.elegirCanal(canal);
        perfiles.save(perfil);
    }

    private List<String> bandeja(String userId) {
        return notificaciones.findByDestinatario(userId, PaginaSolicitada.primera(20)).elementos().stream()
                .map(n -> n.tipo()).toList();
    }

    @Test
    void caso8_unEventoSeEnviaPorEmailAlClienteYPorPushAlProveedor() {
        notificador.actualizar(new SesionCompletada(ContratacionId.generate(), CLIENTE, PROVEEDOR, AHORA));

        assertEquals(1, email.enviados().size());
        assertEquals("user-mateo@skillswap.demo", email.enviados().get(0).para());
        assertEquals("[SkillSwap] Sesión completada", email.enviados().get(0).asunto());
        assertEquals(1, push.enviados().size());
        assertEquals("dispositivo-user-ana", push.enviados().get(0).tokenDispositivo());
        assertTrue(push.enviados().get(0).texto().startsWith("Sesión completada: "));
        assertEquals(List.of("SESION_COMPLETADA"), bandeja(CLIENTE));
        assertEquals(List.of("SESION_COMPLETADA"), bandeja(PROVEEDOR));
    }

    @Test
    void caso8_deParticipeAParticipeConElRecorridoCompletoDeLaContratacion() {
        var publicaciones = new PublicacionRepositoryEnMemoria(unidadDeTrabajo);
        var wallets = new WalletRepositoryEnMemoria(unidadDeTrabajo);
        wallets.save(Wallet.abrir(CLIENTE, new SkillCoins(500), AHORA));
        wallets.save(Wallet.abrir(PROVEEDOR, new SkillCoins(0), AHORA));
        var publicacion = Publicacion.crearBorrador(PROVEEDOR, TipoPublicacion.OFERTA);
        publicacion.editar("Mentoría", "Detalle", "Programación", Modalidad.REMOTA, Duration.ofHours(1), 1,
                new SkillCoins(100), Set.of());
        var franja = publicacion.agregarFranja(AHORA.plus(Duration.ofDays(3)), AHORA.plus(Duration.ofDays(3).plusHours(1)));
        publicacion.publicar(PROVEEDOR, AHORA);
        publicaciones.save(publicacion);
        var publicador = new PublicadorEventos();
        publicador.suscribir(notificador);
        var contrataciones = new ContratacionService(new ContratacionRepositoryEnMemoria(unidadDeTrabajo),
                publicaciones, wallets, perfiles, new RetencionDePagos(),
                new PoliticaReintegroFactory(ReintegroPorAnticipacion.conUmbralesPorDefecto()), unidadDeTrabajo,
                publicador, Clock.fixed(AHORA, ZoneOffset.UTC));

        var id = contrataciones.solicitar(new SolicitarContratacionCommand(CLIENTE,
                publicacion.id().valor().toString(), franja.valor().toString(), "Hola", "Aprender")).id();
        contrataciones.aceptar(PROVEEDOR, id);
        contrataciones.pagar(CLIENTE, id);
        contrataciones.iniciar(PROVEEDOR, id);
        contrataciones.completar(PROVEEDOR, id);

        assertEquals(List.of("SESION_COMPLETADA", "CONTRATACION_ACEPTADA"), bandeja(CLIENTE));
        assertEquals(List.of("SESION_COMPLETADA", "PAGO_CONFIRMADO", "SOLICITUD_RECIBIDA"), bandeja(PROVEEDOR));
        assertEquals(2, email.enviados().size(), "aceptación y sesión completada, al cliente");
        assertEquals(3, push.enviados().size(), "solicitud, pago y sesión completada, al proveedor");
    }

    @Test
    void conElCanalInternoSoloQuedaEnLaBandeja() {
        guardarPerfil(PROVEEDOR, CanalNotificacion.INTERNA);

        notificador.actualizar(new PagoConfirmado(ContratacionId.generate(), CLIENTE, PROVEEDOR, AHORA));

        assertEquals(List.of("PAGO_CONFIRMADO"), bandeja(PROVEEDOR));
        assertTrue(email.enviados().isEmpty());
        assertTrue(push.enviados().isEmpty());
    }

    @Test
    void sinPerfilSeUsaElCanalInterno() {
        notificador.actualizar(new PagoConfirmado(ContratacionId.generate(), CLIENTE, "user-sin-perfil", AHORA));

        assertEquals(List.of("PAGO_CONFIRMADO"), bandeja("user-sin-perfil"));
        assertTrue(push.enviados().isEmpty());
    }

    @Test
    void siFallaUnEnvioLaBandejaQuedaYElOtroInteresadoLoRecibe() {
        EnvioNotificacion emailCaido = (destinatario, titulo, detalle) -> {
            throw new IllegalStateException("el servidor de email no responde");
        };
        var conEmailCaido = new NotificadorDeInteresados(notificaciones, perfiles,
                new CanalEnvioFactory(emailCaido, new PushAdapter(push)));

        assertDoesNotThrow(() -> conEmailCaido.actualizar(
                new SesionCompletada(ContratacionId.generate(), CLIENTE, PROVEEDOR, AHORA)));

        assertEquals(List.of("SESION_COMPLETADA"), bandeja(CLIENTE));
        assertEquals(1, push.enviados().size());
    }
}
