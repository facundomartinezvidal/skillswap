package ar.edu.uade.skillswap.application.perfil;

import ar.edu.uade.skillswap.application.perfil.dto.CrearPerfilCommand;
import ar.edu.uade.skillswap.application.perfil.dto.DisponibilidadDTO;
import ar.edu.uade.skillswap.application.perfil.dto.EditarPerfilCommand;
import ar.edu.uade.skillswap.application.perfil.exception.PerfilDuplicadoException;
import ar.edu.uade.skillswap.application.shared.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.domain.contratacion.Contratacion;
import ar.edu.uade.skillswap.domain.contratacion.Solicitud;
import ar.edu.uade.skillswap.domain.perfil.CanalNotificacion;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.domain.perfil.exception.DisponibilidadInvalidaException;
import ar.edu.uade.skillswap.domain.perfil.exception.PerfilInactivoException;
import ar.edu.uade.skillswap.domain.publicacion.FranjaId;
import ar.edu.uade.skillswap.domain.publicacion.Modalidad;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.publicacion.TipoPublicacion;
import ar.edu.uade.skillswap.domain.shared.Puntaje;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.shared.TransicionInvalidaException;
import ar.edu.uade.skillswap.infrastructure.memoria.ContratacionRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.PerfilRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.PublicacionRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.UnidadDeTrabajoEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.WalletRepositoryEnMemoria;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Casos de uso del perfil: alta con Wallet, edición, baja lógica (regla 9) y perfil público. */
class PerfilServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");
    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final long BONO = 1000;
    private static final DisponibilidadDTO LUNES_10_A_12 =
            new DisponibilidadDTO(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0));

    private final PerfilRepositoryEnMemoria perfiles = new PerfilRepositoryEnMemoria();
    private final WalletRepositoryEnMemoria wallets = new WalletRepositoryEnMemoria();
    private final PublicacionRepositoryEnMemoria publicaciones = new PublicacionRepositoryEnMemoria();
    private final ContratacionRepositoryEnMemoria contrataciones = new ContratacionRepositoryEnMemoria();
    /** Servicio de identidad simulado: conoce solo a estos usuarios. */
    private final ServicioIdentidad identidad = userId -> Set.of("user-ana", "user-mateo").contains(userId);
    private final PerfilService service = new PerfilService(perfiles, wallets, publicaciones, contrataciones,
            identidad, new UnidadDeTrabajoEnMemoria(), new SkillCoins(BONO), Clock.fixed(AHORA, ZoneOffset.UTC));

    private void crearAna() {
        service.crearPerfil(new CrearPerfilCommand("user-ana", TipoPerfil.PROFESIONAL, "Ana", BUENOS_AIRES));
    }

    @Test
    void crearPerfilLeAbreLaWalletConElBonoDeBienvenida() {
        var perfil = service.crearPerfil(
                new CrearPerfilCommand("user-ana", TipoPerfil.PROFESIONAL, "Ana", BUENOS_AIRES));

        assertEquals("user-ana", perfil.userId());
        assertEquals(TipoPerfil.PROFESIONAL, perfil.tipo());
        assertEquals(CanalNotificacion.INTERNA, perfil.canal());
        assertEquals(0, perfil.cantidadCalificaciones());
        assertTrue(perfil.activo());
        assertEquals(new SkillCoins(BONO), wallets.findByTitular("user-ana").orElseThrow().saldo());
    }

    @Test
    void elUsuarioTieneQueExistirEnElServicioDeIdentidad() {
        var command = new CrearPerfilCommand("user-desconocido", TipoPerfil.PROFESIONAL, "X", BUENOS_AIRES);

        assertThrows(RecursoNoEncontradoException.class, () -> service.crearPerfil(command));
        assertTrue(wallets.findByTitular("user-desconocido").isEmpty());
    }

    @Test
    void unUsuarioTieneUnSoloPerfilYNoRecibeOtroBono() {
        crearAna();

        assertThrows(PerfilDuplicadoException.class, () -> crearAna());
        assertEquals(new SkillCoins(BONO), wallets.findByTitular("user-ana").orElseThrow().saldo());
    }

    @Test
    void editarReemplazaDatosHabilidadesEIntereses() {
        crearAna();

        var editado = service.editarPerfil("user-ana", new EditarPerfilCommand("Ana López", "Mentora de Java",
                ZoneId.of("UTC"), Set.of("java", "testing"), null));

        assertEquals("Ana López", editado.nombreVisible());
        assertEquals("Mentora de Java", editado.biografia());
        assertEquals(Set.of("java", "testing"), editado.habilidades());
        assertEquals(Set.of(), editado.intereses());
        assertEquals("Ana López", service.verPerfil("user-ana").nombreVisible());
    }

    @Test
    void laDisponibilidadNoSePuedeSuperponer() {
        crearAna();
        service.agregarDisponibilidad("user-ana", LUNES_10_A_12);

        assertThrows(DisponibilidadInvalidaException.class, () -> service.agregarDisponibilidad("user-ana",
                new DisponibilidadDTO(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(13, 0))));
        assertEquals(List.of(LUNES_10_A_12), service.verPerfil("user-ana").disponibilidad());

        assertTrue(service.quitarDisponibilidad("user-ana", LUNES_10_A_12).disponibilidad().isEmpty());
    }

    @Test
    void elegirElCanalDeNotificacion() {
        crearAna();

        assertEquals(CanalNotificacion.PUSH, service.elegirCanal("user-ana", CanalNotificacion.PUSH).canal());
        assertEquals(CanalNotificacion.PUSH, service.verPerfil("user-ana").canal());
    }

    @Test
    void laBajaEsLogicaYDespuesNoSePuedeEditar() {
        crearAna();

        service.darDeBaja("user-ana", "Me tomo un año sabático");

        var perfil = service.verPerfil("user-ana");
        assertFalse(perfil.activo());
        assertThrows(PerfilInactivoException.class,
                () -> service.elegirCanal("user-ana", CanalNotificacion.EMAIL));
        assertThrows(TransicionInvalidaException.class, () -> service.darDeBaja("user-ana", "otra vez"));
    }

    @Test
    void elPerfilPublicoMuestraReputacionPublicacionesActivasYCompletadas() {
        crearAna();
        var ana = perfiles.findByUserId("user-ana").orElseThrow();
        ana.registrarCalificacion(new Puntaje(5));
        ana.registrarCalificacion(new Puntaje(4));
        perfiles.save(ana);

        var activa = Publicacion.crearBorrador("user-ana", TipoPublicacion.OFERTA);
        activa.editar("Mentoría", "Detalle", "Programación", Modalidad.REMOTA, Duration.ofHours(1), 1,
                new SkillCoins(100), Set.of());
        activa.agregarFranja(AHORA.plus(Duration.ofDays(3)), AHORA.plus(Duration.ofDays(3).plusHours(1)));
        activa.publicar("user-ana", AHORA);
        publicaciones.save(activa);
        publicaciones.save(Publicacion.crearBorrador("user-ana", TipoPublicacion.OFERTA));

        var completada = Contratacion.solicitar("user-mateo", "user-ana", PublicacionId.generate(),
                new Solicitud("Hola", "Aprender", FranjaId.generate()), AHORA.plus(Duration.ofDays(1)),
                new SkillCoins(100), AHORA);
        completada.aceptar("user-ana", AHORA);
        completada.pagar("user-mateo", AHORA);
        completada.iniciar("user-ana", AHORA);
        completada.completar("user-ana", AHORA);
        contrataciones.save(completada);

        var perfil = service.verPerfil("user-ana");

        assertEquals(4.5, perfil.reputacion());
        assertEquals(2, perfil.cantidadCalificaciones());
        assertEquals(1, perfil.publicacionesActivas().size(), "el borrador no es una publicación activa");
        assertEquals("PUBLICADA", perfil.publicacionesActivas().get(0).estado());
        assertEquals(1, perfil.contratacionesCompletadas());
    }

    @Test
    void unPerfilInexistenteSeInformaComoNoEncontrado() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.verPerfil("user-nadie"));
    }
}
