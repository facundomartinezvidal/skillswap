package ar.edu.uade.skillswap.application.busqueda;

import ar.edu.uade.skillswap.application.busqueda.recomendacion.AfinidadPorHabilidades;
import ar.edu.uade.skillswap.application.publicacion.PublicacionService;
import ar.edu.uade.skillswap.application.publicacion.dto.DatosPublicacionCommand;
import ar.edu.uade.skillswap.application.publicacion.dto.PublicacionDTO;
import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.domain.publicacion.Modalidad;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.publicacion.TipoPublicacion;
import ar.edu.uade.skillswap.domain.publicacion.decorator.DescuentoFijo;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.domain.shared.Puntaje;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.infrastructure.memoria.BuscadorPublicacionesEnMemoria;
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

/** Búsqueda con filtros, orden y paginación. Cubre el caso de aceptación 1. */
class BusquedaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");
    private static final String ANA = "user-ana";
    private static final String LUIS = "user-luis";

    private final UnidadDeTrabajoEnMemoria unidadDeTrabajo = new UnidadDeTrabajoEnMemoria();
    private final PublicacionRepositoryEnMemoria publicaciones = new PublicacionRepositoryEnMemoria(unidadDeTrabajo);
    private final PerfilRepositoryEnMemoria perfiles = new PerfilRepositoryEnMemoria(unidadDeTrabajo);
    private final BusquedaService service = new BusquedaService(
            new BuscadorPublicacionesEnMemoria(publicaciones, perfiles), publicaciones, perfiles,
            new AfinidadPorHabilidades(perfiles));

    @BeforeEach
    void prepararDatos() {
        var ana = Perfil.crear(ANA, TipoPerfil.PROFESIONAL, "Ana", ZoneId.of("UTC"));
        ana.definirHabilidades(Set.of("Java", "Testing"));
        ana.registrarCalificacion(new Puntaje(5));
        perfiles.save(ana);
        var luis = Perfil.crear(LUIS, TipoPerfil.PROFESIONAL, "Luis", ZoneId.of("UTC"));
        luis.definirHabilidades(Set.of("Figma"));
        luis.registrarCalificacion(new Puntaje(3));
        perfiles.save(luis);

        publicar(ANA, "Mentoría de Java", "Programación", Modalidad.REMOTA, 100, Set.of("backend"), 1);
        publicar(LUIS, "Revisión de portfolio", "Diseño", Modalidad.PRESENCIAL, 50, Set.of("ux"), 2);
        publicar(ANA, "Arquitectura con Java y Spring", "Programación", Modalidad.REMOTA, 300, Set.of("java"), 3);
        publicaciones.save(Publicacion.crearBorrador(ANA, TipoPublicacion.OFERTA));
    }

    /** Publica directo con el dominio; {@code diasAtras} da la fecha de publicación (más chico, más reciente). */
    private void publicar(String autor, String titulo, String categoria, Modalidad modalidad, long precio,
                          Set<String> etiquetas, int diasAtras) {
        var p = Publicacion.crearBorrador(autor, TipoPublicacion.OFERTA);
        p.editar(titulo, "Sesión práctica", categoria, modalidad, Duration.ofHours(1), 2, new SkillCoins(precio),
                etiquetas);
        p.agregarFranja(AHORA.plus(Duration.ofDays(5)), AHORA.plus(Duration.ofDays(5).plusHours(1)));
        p.publicar(autor, AHORA.minus(Duration.ofDays(diasAtras)));
        publicaciones.save(p);
    }

    private List<String> titulos(CriteriosBusqueda criterios) {
        return service.buscar(criterios, PaginaSolicitada.primera(10)).elementos().stream()
                .map(PublicacionDTO::titulo).toList();
    }

    private static CriteriosBusqueda criterios() {
        return new CriteriosBusqueda(null, null, null, null, null, null, null, null);
    }

    @Test
    void caso1_publicarUnaMentoriaValidaYEncontrarlaConFiltros() {
        var publicacionService = new PublicacionService(publicaciones, perfiles, Clock.fixed(AHORA, ZoneOffset.UTC));
        var borrador = publicacionService.crearBorrador(ANA, new DatosPublicacionCommand("Mentoría de testing",
                "TDD desde cero", "Programación", Modalidad.REMOTA, Duration.ofHours(1), 1, 80L, Set.of("junit")));
        publicacionService.agregarFranja(ANA, borrador.id(), AHORA.plus(Duration.ofDays(2)),
                AHORA.plus(Duration.ofDays(2).plusHours(1)));
        publicacionService.publicar(ANA, borrador.id());

        var encontradas = titulos(new CriteriosBusqueda("testing", "programación", Modalidad.REMOTA, 50L, 100L,
                "junit", 4.0, OrdenBusqueda.RELEVANCIA));

        assertEquals(List.of("Mentoría de testing"), encontradas);
    }

    @Nested
    class Filtros {

        @Test
        void sinFiltrosTraeSoloLasPublicadas() {
            assertEquals(3, service.buscar(criterios(), PaginaSolicitada.primera(10)).total());
        }

        @Test
        void porTextoEnTituloDescripcionOEtiquetas() {
            assertEquals(List.of("Mentoría de Java", "Arquitectura con Java y Spring"),
                    titulos(new CriteriosBusqueda("java", null, null, null, null, null, null, null)));
            assertEquals(List.of("Revisión de portfolio"),
                    titulos(new CriteriosBusqueda("UX", null, null, null, null, null, null, null)));
        }

        @Test
        void porCategoriaYModalidad() {
            assertEquals(List.of("Revisión de portfolio"),
                    titulos(new CriteriosBusqueda(null, "Diseño", null, null, null, null, null, null)));
            assertEquals(2, titulos(new CriteriosBusqueda(null, null, Modalidad.REMOTA, null, null, null, null, null))
                    .size());
        }

        @Test
        void porRangoDePrecioFinalConPromociones() {
            var cara = publicaciones.findPublicadas().stream()
                    .filter(p -> p.titulo().startsWith("Arquitectura")).findFirst().orElseThrow();
            cara.agregarPromocion(new DescuentoFijo(new SkillCoins(220)));
            publicaciones.save(cara);

            assertEquals(List.of("Mentoría de Java", "Arquitectura con Java y Spring"),
                    titulos(new CriteriosBusqueda(null, null, null, 60L, 100L, null, null, null)));
        }

        @Test
        void porHabilidadDeLaPublicacionODelAutor() {
            assertEquals(List.of("Revisión de portfolio"),
                    titulos(new CriteriosBusqueda(null, null, null, null, null, "figma", null, null)));
            assertEquals(2, titulos(new CriteriosBusqueda(null, null, null, null, null, "Java", null, null)).size());
        }

        @Test
        void porReputacionMinimaDelAutor() {
            assertEquals(2, titulos(new CriteriosBusqueda(null, null, null, null, null, null, 4.0, null)).size());
            assertEquals(3, titulos(new CriteriosBusqueda(null, null, null, null, null, null, 3.0, null)).size());
        }
    }

    @Nested
    class OrdenYPaginacion {

        @Test
        void porMejorReputacionMenorPrecioOFechaMasReciente() {
            assertEquals("Revisión de portfolio", titulos(new CriteriosBusqueda(null, null, null, null, null, null,
                    null, OrdenBusqueda.MEJOR_REPUTACION)).get(2));
            assertEquals(List.of("Revisión de portfolio", "Mentoría de Java", "Arquitectura con Java y Spring"),
                    titulos(new CriteriosBusqueda(null, null, null, null, null, null, null, OrdenBusqueda.MENOR_PRECIO)));
            assertEquals(List.of("Mentoría de Java", "Revisión de portfolio", "Arquitectura con Java y Spring"),
                    titulos(new CriteriosBusqueda(null, null, null, null, null, null, null, OrdenBusqueda.MAS_RECIENTE)));
        }

        @Test
        void porRelevanciaPrimeroLasQueTienenElTextoEnElTitulo() {
            var titulos = titulos(new CriteriosBusqueda("portfolio", null, null, null, null, null, null, null));

            assertEquals(List.of("Revisión de portfolio"), titulos);
        }

        @Test
        void losResultadosSePaginanYTraenAutorYReputacion() {
            var pagina = service.buscar(criterios(), new PaginaSolicitada(1, 2));

            assertEquals(3, pagina.total());
            assertEquals(1, pagina.elementos().size());
            var primera = service.buscar(criterios(), PaginaSolicitada.primera(1)).elementos().get(0);
            assertNotNull(primera.autorNombre());
            assertTrue(primera.reputacionAutor() > 0);
        }
    }

    @Test
    void lasCategoriasSonLasDeLasPublicadasSinRepetir() {
        assertEquals(List.of("Diseño", "Programación"), service.categorias());
    }
}
