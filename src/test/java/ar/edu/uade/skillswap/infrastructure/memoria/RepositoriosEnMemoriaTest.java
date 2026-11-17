package ar.edu.uade.skillswap.infrastructure.memoria;

import ar.edu.uade.skillswap.domain.contratacion.Contratacion;
import ar.edu.uade.skillswap.domain.contratacion.Solicitud;
import ar.edu.uade.skillswap.domain.publicacion.FranjaId;
import ar.edu.uade.skillswap.domain.publicacion.Modalidad;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.publicacion.TipoPublicacion;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.wallet.Wallet;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Los repositorios en memoria se comportan como una base: devuelven copias, así que un cambio
 * que no se guardó no se ve. En eso se apoya la atomicidad de los casos de uso en los tests.
 */
class RepositoriosEnMemoriaTest {

    private static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");

    private final UnidadDeTrabajoEnMemoria unidadDeTrabajo = new UnidadDeTrabajoEnMemoria();

    private static Publicacion publicada(String autor, String categoria) {
        var publicacion = Publicacion.crearBorrador(autor, TipoPublicacion.OFERTA);
        publicacion.editar("Mentoría", "Detalle", categoria, Modalidad.REMOTA, Duration.ofHours(1), 1,
                new SkillCoins(100), Set.of());
        publicacion.agregarFranja(AHORA.plus(Duration.ofDays(3)), AHORA.plus(Duration.ofDays(3).plusHours(1)));
        publicacion.publicar(autor, AHORA);
        return publicacion;
    }

    private static Contratacion pendiente(String cliente, PublicacionId publicacion) {
        return Contratacion.solicitar(cliente, "user-ana", publicacion,
                new Solicitud("Hola", "Aprender", FranjaId.generate()), AHORA.plus(Duration.ofDays(3)),
                new SkillCoins(100), AHORA);
    }

    @Test
    void unCambioSinGuardarNoSeVe() {
        var repositorio = new PublicacionRepositoryEnMemoria(unidadDeTrabajo);
        var publicacion = publicada("user-ana", "Programación");
        repositorio.save(publicacion);

        var leida = repositorio.findById(publicacion.id()).orElseThrow();
        leida.reservarCupo(leida.franjas().get(0).id());
        leida.pausar("user-ana", "vacaciones", AHORA);

        var deNuevo = repositorio.findById(publicacion.id()).orElseThrow();
        assertNotSame(leida, deNuevo);
        assertEquals("PUBLICADA", deNuevo.estado().nombre());
        assertEquals(0, deNuevo.franjas().get(0).reservados());

        repositorio.save(leida);
        assertEquals("PAUSADA", repositorio.findById(publicacion.id()).orElseThrow().estado().nombre());
    }

    @Test
    void publicacionesPorAutorPublicadasYCategorias() {
        var repositorio = new PublicacionRepositoryEnMemoria(unidadDeTrabajo);
        repositorio.save(publicada("user-ana", "Programación"));
        repositorio.save(publicada("user-ana", "Diseño"));
        repositorio.save(publicada("user-luis", "Programación"));
        var borrador = Publicacion.crearBorrador("user-ana", TipoPublicacion.OFERTA);
        repositorio.save(borrador);

        assertEquals(3, repositorio.findByAutor("user-ana", PaginaSolicitada.primera(10)).total());
        assertEquals(3, repositorio.findPublicadas().size());
        assertEquals(List.of("Diseño", "Programación"), repositorio.categorias());
    }

    @Test
    void contratacionesPorParticipanteCompletadasYAceptadasRecientes() {
        var repositorio = new ContratacionRepositoryEnMemoria(unidadDeTrabajo);
        var publicacion = PublicacionId.generate();
        var aceptada = pendiente("user-mateo", publicacion);
        aceptada.aceptar("user-ana", AHORA);
        repositorio.save(aceptada);
        repositorio.save(pendiente("user-lucia", publicacion));

        assertEquals(1, repositorio.findByParticipante("user-mateo", PaginaSolicitada.primera(10)).total());
        assertEquals(2, repositorio.findByParticipante("user-ana", PaginaSolicitada.primera(10)).total());
        assertEquals(0, repositorio.countCompletadasDe("user-ana"));
        assertEquals(Map.of(publicacion, 1L), repositorio.countAceptadasDesde(AHORA.minus(Duration.ofDays(30))));
        assertEquals(Map.of(), repositorio.countAceptadasDesde(AHORA.plusSeconds(1)));
        assertTrue(repositorio.findById(aceptada.id()).orElseThrow().extraerEventos().isEmpty());
    }

    @Test
    void activasDeUnAutorYVariasPorId() {
        var repositorio = new PublicacionRepositoryEnMemoria(unidadDeTrabajo);
        var activa = publicada("user-ana", "Programación");
        var deOtro = publicada("user-luis", "Diseño");
        repositorio.save(activa);
        repositorio.save(deOtro);
        repositorio.save(Publicacion.crearBorrador("user-ana", TipoPublicacion.OFERTA));

        assertEquals(List.of(activa.id()),
                repositorio.findActivasByAutor("user-ana").stream().map(Publicacion::id).toList());
        assertEquals(2, repositorio.findByIds(
                List.of(activa.id(), deOtro.id(), activa.id(), PublicacionId.generate())).size());
    }

    @Test
    void enUnaTransaccionQueFallaNoSeGuardaNada() {
        var publicaciones = new PublicacionRepositoryEnMemoria(unidadDeTrabajo);
        var wallets = new WalletRepositoryEnMemoria(unidadDeTrabajo);
        var publicacion = publicada("user-ana", "Programación");

        assertThrows(IllegalStateException.class, () -> unidadDeTrabajo.ejecutar(() -> {
            publicaciones.save(publicacion);
            wallets.save(Wallet.abrir("user-mateo", new SkillCoins(500), AHORA));
            throw new IllegalStateException("falla después de guardar");
        }));

        assertTrue(publicaciones.findById(publicacion.id()).isEmpty());
        assertTrue(wallets.findByTitular("user-mateo").isEmpty());
        assertEquals(0, wallets.findMovimientos("user-mateo", PaginaSolicitada.primera(10)).total());
    }

    @Test
    void enUnaTransaccionQueTerminaBienSeGuardaTodoAlFinal() {
        var publicaciones = new PublicacionRepositoryEnMemoria(unidadDeTrabajo);
        var publicacion = publicada("user-ana", "Programación");

        var vistoAdentro = unidadDeTrabajo.ejecutar(() -> {
            publicaciones.save(publicacion);
            return publicaciones.findById(publicacion.id()).isPresent();
        });

        assertFalse(vistoAdentro, "adentro de la transacción todavía no está aplicado");
        assertTrue(publicaciones.findById(publicacion.id()).isPresent());
    }

    @Test
    void lasWalletsGuardanSaldoYAgreganLosMovimientosNuevos() {
        var repositorio = new WalletRepositoryEnMemoria(unidadDeTrabajo);
        repositorio.save(Wallet.abrir("user-mateo", new SkillCoins(500), AHORA));

        var leida = repositorio.findByTitular("user-mateo").orElseThrow();
        assertTrue(leida.movimientosNuevos().isEmpty());
        leida.acreditarDemo(new SkillCoins(50), AHORA);
        repositorio.save(leida);

        assertEquals(new SkillCoins(550), repositorio.findByTitular("user-mateo").orElseThrow().saldo());
        var movimientos = repositorio.findMovimientos("user-mateo", PaginaSolicitada.primera(10));
        assertEquals(2, movimientos.total());
        assertEquals(new SkillCoins(550), movimientos.elementos().get(0).saldoResultante(), "el más nuevo primero");
        assertTrue(repositorio.garantia().esGarantia());
        assertTrue(repositorio.findByTitular("user-nadie").isEmpty());
    }
}
