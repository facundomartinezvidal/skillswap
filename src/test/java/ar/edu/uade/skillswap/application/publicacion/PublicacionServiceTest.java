package ar.edu.uade.skillswap.application.publicacion;

import ar.edu.uade.skillswap.application.publicacion.dto.DatosPublicacionCommand;
import ar.edu.uade.skillswap.application.publicacion.dto.PromocionDTO;
import ar.edu.uade.skillswap.application.publicacion.dto.PublicacionDTO;
import ar.edu.uade.skillswap.application.shared.exception.PermisoDenegadoException;
import ar.edu.uade.skillswap.application.shared.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.domain.perfil.exception.PerfilInactivoException;
import ar.edu.uade.skillswap.domain.publicacion.Modalidad;
import ar.edu.uade.skillswap.domain.publicacion.TipoPublicacion;
import ar.edu.uade.skillswap.domain.publicacion.exception.PublicacionIncompletaException;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.domain.shared.Puntaje;
import ar.edu.uade.skillswap.domain.shared.TransicionInvalidaException;
import ar.edu.uade.skillswap.infrastructure.memoria.PerfilRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.PublicacionRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.UnidadDeTrabajoEnMemoria;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Casos de uso de las publicaciones. Cubre los casos de aceptación 2 y 9, y la regla 10 (solo el autor). */
class PublicacionServiceTest {

    private static final String ANA = "user-ana";
    private static final String ORGANIZACION = "user-acme";
    private static final String OTRO = "user-mateo";
    private static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");
    private static final Instant INICIO = AHORA.plus(Duration.ofDays(3));
    private static final Instant FIN = INICIO.plus(Duration.ofHours(1));
    private static final DatosPublicacionCommand COMPLETA = new DatosPublicacionCommand("Mentoría de Java",
            "Revisión de código", "Programación", Modalidad.REMOTA, Duration.ofHours(1), 2, 100L, Set.of("java"));

    private final UnidadDeTrabajoEnMemoria unidadDeTrabajo = new UnidadDeTrabajoEnMemoria();
    private final PublicacionRepositoryEnMemoria publicaciones = new PublicacionRepositoryEnMemoria(unidadDeTrabajo);
    private final PerfilRepositoryEnMemoria perfiles = new PerfilRepositoryEnMemoria(unidadDeTrabajo);
    private final PublicacionService service =
            new PublicacionService(publicaciones, perfiles, Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void crearPerfiles() {
        var ana = Perfil.crear(ANA, TipoPerfil.PROFESIONAL, "Ana", ZoneId.of("UTC"));
        ana.registrarCalificacion(new Puntaje(4));
        perfiles.save(ana);
        perfiles.save(Perfil.crear(ORGANIZACION, TipoPerfil.ORGANIZACION, "Acme", ZoneId.of("UTC")));
        perfiles.save(Perfil.crear(OTRO, TipoPerfil.PROFESIONAL, "Mateo", ZoneId.of("UTC")));
    }

    private PublicacionDTO borradorConFranja() {
        var borrador = service.crearBorrador(ANA, COMPLETA);
        return service.agregarFranja(ANA, borrador.id(), INICIO, FIN);
    }

    private PublicacionDTO publicada() {
        return service.publicar(ANA, borradorConFranja().id());
    }

    private String estadoGuardado(PublicacionDTO publicacion) {
        return service.verPublicacion(ANA, publicacion.id()).estado();
    }

    @Nested
    class CrearYPublicar {

        @Test
        void unProfesionalPublicaUnaOfertaCompleta() {
            var publicacion = publicada();

            assertEquals("PUBLICADA", publicacion.estado());
            assertEquals(TipoPublicacion.OFERTA, publicacion.tipo());
            assertEquals("Ana", publicacion.autorNombre());
            assertEquals(4.0, publicacion.reputacionAutor());
            assertEquals(100L, publicacion.precioFinal());
            assertEquals(1, publicacion.franjas().size());
            assertEquals(2, publicacion.franjas().get(0).lugaresLibres());
            assertEquals("PUBLICADA", estadoGuardado(publicacion));
        }

        @Test
        void unaOrganizacionCreaUnDesafio() {
            var desafio = service.crearBorrador(ORGANIZACION, COMPLETA);

            assertEquals(TipoPublicacion.DESAFIO, desafio.tipo());
            assertEquals("BORRADOR", desafio.estado());
        }

        @Test
        void caso2_seRechazaPublicarUnaOfertaIncompleta() {
            var sinPrecioNiFranja = new DatosPublicacionCommand("Mentoría", null, "Programación",
                    Modalidad.REMOTA, Duration.ofHours(1), 1, null, null);
            var borrador = service.crearBorrador(ANA, sinPrecioNiFranja);

            var error = assertThrows(PublicacionIncompletaException.class, () -> service.publicar(ANA, borrador.id()));

            assertEquals(List.of("precio", "al menos una franja de disponibilidad"), error.faltantes());
            assertEquals("BORRADOR", estadoGuardado(borrador));
            assertNull(borrador.precioFinal());
        }

        @Test
        void sinPerfilOConElPerfilDadoDeBajaNoSePublica() {
            assertThrows(RecursoNoEncontradoException.class, () -> service.crearBorrador("user-nadie", COMPLETA));

            var borrador = borradorConFranja();
            var ana = perfiles.findByUserId(ANA).orElseThrow();
            ana.darDeBaja(ANA, "me voy", AHORA);
            perfiles.save(ana);

            assertThrows(PerfilInactivoException.class, () -> service.crearBorrador(ANA, COMPLETA));
            assertThrows(PerfilInactivoException.class, () -> service.publicar(ANA, borrador.id()));
        }

        @Test
        void unaPublicacionPublicadaNoSeEdita() {
            var publicacion = publicada();

            assertThrows(TransicionInvalidaException.class, () -> service.editar(ANA, publicacion.id(), COMPLETA));
        }
    }

    @Nested
    class Permisos {

        @Test
        void soloElAutorModificaSuPublicacion() {
            var borrador = borradorConFranja();
            var publicacion = publicada();
            var promocion = new PromocionDTO(PromocionDTO.Tipo.FIJO, 10);

            assertThrows(PermisoDenegadoException.class, () -> service.editar(OTRO, borrador.id(), COMPLETA));
            assertThrows(PermisoDenegadoException.class, () -> service.agregarFranja(OTRO, borrador.id(), INICIO, FIN));
            assertThrows(PermisoDenegadoException.class, () -> service.publicar(OTRO, borrador.id()));
            assertThrows(PermisoDenegadoException.class, () -> service.pausar(OTRO, publicacion.id(), "no"));
            assertThrows(PermisoDenegadoException.class, () -> service.cancelar(OTRO, publicacion.id(), "no"));
            assertThrows(PermisoDenegadoException.class,
                    () -> service.agregarPromocion(OTRO, publicacion.id(), promocion));
            assertEquals("PUBLICADA", estadoGuardado(publicacion));
        }

        @Test
        void unBorradorOUnaPausadaSoloLaVeSuAutor() {
            var borrador = borradorConFranja();
            assertEquals("BORRADOR", service.verPublicacion(ANA, borrador.id()).estado());
            assertThrows(RecursoNoEncontradoException.class, () -> service.verPublicacion(OTRO, borrador.id()));

            service.publicar(ANA, borrador.id());
            assertEquals("PUBLICADA", service.verPublicacion(OTRO, borrador.id()).estado());

            service.pausar(ANA, borrador.id(), "vacaciones");
            assertThrows(RecursoNoEncontradoException.class, () -> service.verPublicacion(OTRO, borrador.id()));
            assertEquals("PAUSADA", service.verPublicacion(ANA, borrador.id()).estado());
        }

        @Test
        void unaPublicacionInexistenteSeInformaComoNoEncontrada() {
            assertThrows(RecursoNoEncontradoException.class, () -> service.verPublicacion(ANA, "no-es-un-id"));
            assertThrows(RecursoNoEncontradoException.class,
                    () -> service.publicar(ANA, "00000000-0000-0000-0000-000000000000"));
        }
    }

    @Nested
    class CambiosDeEstado {

        @Test
        void pausarReanudarYCerrarQuedanEnElHistorial() {
            var publicacion = publicada();

            assertEquals("PAUSADA", service.pausar(ANA, publicacion.id(), "vacaciones").estado());
            assertEquals("PUBLICADA", service.reanudar(ANA, publicacion.id()).estado());
            assertEquals("CERRADA", service.cerrar(ANA, publicacion.id(), null).estado());
            assertThrows(TransicionInvalidaException.class, () -> service.reanudar(ANA, publicacion.id()));
            assertEquals("CERRADA", estadoGuardado(publicacion));
        }

        @Test
        void cancelarExigeMotivoYLoGuarda() {
            var publicacion = publicada();

            assertThrows(IllegalArgumentException.class, () -> service.cancelar(ANA, publicacion.id(), " "));
            var cancelada = service.cancelar(ANA, publicacion.id(), "Ya no la dicto");

            assertEquals("CANCELADA", cancelada.estado());
            assertEquals("Ya no la dicto", cancelada.motivoCancelacion());
        }
    }

    @Nested
    class Promociones {

        @Test
        void caso9_lasPromocionesSeAcumulanSinBajarDeCero() {
            var publicacion = publicada();

            var conPorcentaje = service.agregarPromocion(ANA, publicacion.id(),
                    new PromocionDTO(PromocionDTO.Tipo.PORCENTUAL, 10));
            assertEquals(90L, conPorcentaje.precioFinal());

            var conFijo = service.agregarPromocion(ANA, publicacion.id(), new PromocionDTO(PromocionDTO.Tipo.FIJO, 200));
            assertEquals(0L, conFijo.precioFinal());
            assertEquals(100L, conFijo.precioBase());
            assertEquals(2, conFijo.promociones().size());
        }

        @Test
        void quitarUnaPromocionVuelveAlPrecioAnterior() {
            var publicacion = publicada();
            var descuento = new PromocionDTO(PromocionDTO.Tipo.FIJO, 30);
            service.agregarPromocion(ANA, publicacion.id(), descuento);

            var sinDescuento = service.quitarPromocion(ANA, publicacion.id(), descuento);

            assertEquals(100L, sinDescuento.precioFinal());
            assertTrue(sinDescuento.promociones().isEmpty());
        }

        @Test
        void unPorcentajeFueraDeRangoSeRechaza() {
            var publicacion = publicada();

            assertThrows(IllegalArgumentException.class, () -> service.agregarPromocion(ANA, publicacion.id(),
                    new PromocionDTO(PromocionDTO.Tipo.PORCENTUAL, 150)));
        }
    }

    @Test
    void listarPropiasTraeLasDelAutorEnCualquierEstadoPaginadas() {
        publicada();
        service.crearBorrador(ANA, COMPLETA);
        service.crearBorrador(OTRO, COMPLETA);

        var pagina = service.listarPropias(ANA, PaginaSolicitada.primera(1));

        assertEquals(2, pagina.total());
        assertEquals(1, pagina.elementos().size());
        assertEquals(2, service.listarPropias(ANA, PaginaSolicitada.primera(10)).elementos().stream()
                .map(PublicacionDTO::autorId).filter(ANA::equals).count());
    }
}
