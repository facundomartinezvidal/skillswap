package ar.edu.uade.skillswap.infrastructure.persistencia;

import ar.edu.uade.skillswap.application.busqueda.CriteriosBusqueda;
import ar.edu.uade.skillswap.application.busqueda.OrdenBusqueda;
import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.publicacion.Modalidad;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.publicacion.TipoPublicacion;
import ar.edu.uade.skillswap.domain.publicacion.decorator.DescuentoPorcentual;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.domain.shared.Puntaje;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.infrastructure.persistencia.publicacion.BuscadorPublicacionesJpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Caso de aceptación 1 contra MySQL: la búsqueda filtra, ordena y pagina en la base. */
class BuscadorPublicacionesJpaTest extends BaseDeDatosDePrueba {

    private static final String ANA = "user-ana";
    private static final String LUCIA = "user-lucia";

    private BuscadorPublicacionesJpa buscador;
    private Publicacion java;
    private Publicacion spring;
    private Publicacion excel;
    private Publicacion fotografia;

    /**
     * Ana (reputación 5, habilidad "Kotlin") publica Java (100, publicada hace 3 días) y Spring (80 con
     * 50 % = 40, hace 1 día). Lucía (reputación 3) publica Excel (60, presencial, hace 2 días) y
     * Fotografía (200, presencial, hace 4 días). Además hay un borrador y una pausada de Ana, que no tienen
     * que aparecer nunca.
     */
    @BeforeEach
    void publicaciones() {
        buscador = new BuscadorPublicacionesJpa(unidadDeTrabajo);
        var ana = guardarPerfil(ANA);
        ana.definirHabilidades(Set.of("Kotlin"));
        ana.registrarCalificacion(new Puntaje(5));
        perfiles.save(ana);
        Perfil lucia = guardarPerfil(LUCIA);
        lucia.registrarCalificacion(new Puntaje(3));
        perfiles.save(lucia);

        java = guardarPublicada(ANA, "Mentoría de Java", "Programación", Modalidad.REMOTA, 100,
                Set.of("backend"), AHORA.minus(Duration.ofDays(3)));
        spring = guardarPublicada(ANA, "Spring para principiantes", "Programación", Modalidad.REMOTA, 80,
                Set.of("java", "web"), AHORA.minus(Duration.ofDays(1)));
        spring.agregarPromocion(new DescuentoPorcentual(50));
        publicaciones.save(spring);
        excel = guardarPublicada(LUCIA, "Excel avanzado", "Oficina", Modalidad.PRESENCIAL, 60, Set.of("planillas"),
                AHORA.minus(Duration.ofDays(2)));
        fotografia = guardarPublicada(LUCIA, "Fotografía", "Arte", Modalidad.PRESENCIAL, 200, Set.of(),
                AHORA.minus(Duration.ofDays(4)));
        publicaciones.save(Publicacion.crearBorrador(ANA, TipoPublicacion.OFERTA));
        var pausada = guardarPublicada(ANA, "Java pausada", "Programación", Modalidad.REMOTA, 10, Set.of("java"), AHORA);
        pausada.pausar(ANA, "Vacaciones", AHORA);
        publicaciones.save(pausada);
    }

    private List<PublicacionId> buscar(CriteriosBusqueda criterios) {
        return buscador.buscar(criterios, PaginaSolicitada.primera(10)).elementos().stream()
                .map(Publicacion::id).toList();
    }

    private static CriteriosBusqueda orden(OrdenBusqueda orden) {
        return new CriteriosBusqueda(null, null, null, null, null, null, null, orden);
    }

    @Test
    void sinFiltrosDevuelveSoloLasPublicadasDeLaMasRecienteALaMasVieja() {
        assertEquals(List.of(spring.id(), excel.id(), java.id(), fotografia.id()), buscar(orden(null)));
    }

    @Test
    void elTextoSeBuscaEnTituloDescripcionYEtiquetasYPrimeroVanLasQueLoTienenEnElTitulo() {
        var criterios = new CriteriosBusqueda("JAVA", null, null, null, null, null, null, OrdenBusqueda.RELEVANCIA);

        assertEquals(List.of(java.id(), spring.id()), buscar(criterios),
                "Java lo tiene en el título; Spring, solo en una etiqueta");
        assertEquals(List.of(excel.id()), buscar(new CriteriosBusqueda("de excel", null, null, null, null, null, null,
                null)), "solo en la descripción");
    }

    @Test
    void filtraPorCategoriaModalidadYRangoDePrecioFinal() {
        assertEquals(List.of(excel.id()), buscar(new CriteriosBusqueda(null, "oficina", null, null, null, null, null,
                null)));
        assertEquals(List.of(excel.id(), fotografia.id()), buscar(new CriteriosBusqueda(null, null,
                Modalidad.PRESENCIAL, null, null, null, null, null)));
        assertEquals(List.of(spring.id(), excel.id()), buscar(new CriteriosBusqueda(null, null, null, 40L, 60L, null,
                null, null)), "Spring vale 40 con la promoción, aunque su precio base sea 80");
    }

    @Test
    void filtraPorHabilidadEnEtiquetasOEnElAutorYPorReputacionMinima() {
        assertEquals(List.of(excel.id()), buscar(new CriteriosBusqueda(null, null, null, null, null, "Planillas",
                null, null)), "por etiqueta");
        assertEquals(List.of(spring.id(), java.id()), buscar(new CriteriosBusqueda(null, null, null, null, null,
                "kotlin", null, null)), "por habilidad de la autora");
        assertEquals(List.of(spring.id(), java.id()), buscar(new CriteriosBusqueda(null, null, null, null, null, null,
                4.0, null)));
    }

    @Test
    void ordenaPorMejorReputacionYPorMenorPrecio() {
        assertEquals(List.of(spring.id(), java.id(), excel.id(), fotografia.id()),
                buscar(orden(OrdenBusqueda.MEJOR_REPUTACION)));
        assertEquals(List.of(spring.id(), excel.id(), java.id(), fotografia.id()),
                buscar(orden(OrdenBusqueda.MENOR_PRECIO)));
        assertEquals(List.of(spring.id(), excel.id(), java.id(), fotografia.id()),
                buscar(orden(OrdenBusqueda.MAS_RECIENTE)));
    }

    @Test
    void paginaEnLaBaseYCuentaElTotal() {
        var segunda = buscador.buscar(orden(OrdenBusqueda.MENOR_PRECIO), new PaginaSolicitada(1, 3));

        assertEquals(4, segunda.total());
        assertEquals(2, segunda.totalPaginas());
        assertEquals(List.of(fotografia.id()), segunda.elementos().stream().map(Publicacion::id).toList());
        assertEquals(new SkillCoins(200), segunda.elementos().get(0).precioFinal());
    }
}
