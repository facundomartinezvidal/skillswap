package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.controllers.BusquedaOperaciones.Criterios;
import ar.edu.uade.skillswap.controllers.BusquedaOperaciones.Orden;
import ar.edu.uade.skillswap.controllers.impl.SkillSwapControllers;
import ar.edu.uade.skillswap.model.Models.EstadoPublicacion;
import ar.edu.uade.skillswap.model.Models.Modalidad;
import ar.edu.uade.skillswap.model.Models.Publicacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

import static ar.edu.uade.skillswap.controllers.EntornoDemo.MATEO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Búsqueda completa y listados paginados de las interfaces del equipo, sobre los datos de demo. */
class ListadosPaginadosTest {

    private static final Criterios SIN_FILTROS = new Criterios(null, null, null, null, null, null, null, null);
    /** Las 6 publicaciones del mock más la mentoría publicada de Mateo. */
    private static final int PUBLICADAS = 7;

    private final EntornoDemo demo = new EntornoDemo();
    private final SkillSwapControllers c = demo.controllers;

    @Test
    void lasPublicacionesPropiasSePaginan() {
        var primera = c.publicaciones().listarPropias(MATEO, 0, 1);
        var segunda = c.publicaciones().listarPropias(MATEO, 1, 1);

        assertEquals(2, primera.total());
        assertEquals(1, primera.elementos().size());
        assertEquals(1, segunda.elementos().size());
        assertTrue(!primera.elementos().get(0).id().equals(segunda.elementos().get(0).id()));
        assertEquals(c.publicaciones().listarPropias(MATEO).size(), primera.total());
    }

    /** Las recomendaciones paginadas siguen el mismo orden que la fila de la pantalla de inicio. */
    @Test
    void lasRecomendacionesSePaginanEnElMismoOrden() {
        var fila = c.recomendaciones().recomendar(EntornoDemo.ANA);
        var primera = c.recomendaciones().recomendar(EntornoDemo.ANA, 0, 2);
        var segunda = c.recomendaciones().recomendar(EntornoDemo.ANA, 1, 2);

        assertEquals(fila.subList(0, 2), primera.elementos());
        assertEquals(fila.get(2), segunda.elementos().get(0));
        assertTrue(primera.total() >= fila.size(), "total: " + primera.total());
        assertEquals(primera.total(), segunda.total());
    }

    @Test
    void buscarPorRangoDePrecioOrdenadoPorMenorPrecio() {
        var pagina = c.busqueda().buscar(new Criterios(null, null, null, 150, 250, null, null, Orden.MENOR_PRECIO),
                0, 10);

        var precios = pagina.elementos().stream().map(Publicacion::precio).toList();
        assertEquals(List.of(150, 160, 220, 234), precios);
        assertEquals(4, pagina.total());
    }

    @Test
    void buscarPorHabilidadModalidadYCategoria() {
        var pagina = c.busqueda().buscar(new Criterios(null, "Tecnología", Modalidad.REMOTA, null, null, "Java",
                null, Orden.RELEVANCIA), 0, 10);

        assertEquals(List.of("Mentoría de Java inicial", "Preparación para entrevistas técnicas"),
                pagina.elementos().stream().map(Publicacion::titulo).sorted().toList());
    }

    @Test
    void buscarPorReputacionMinimaOrdenadoPorMejorReputacion() {
        var pagina = c.busqueda().buscar(new Criterios(null, null, null, null, null, null, 4.5,
                Orden.MEJOR_REPUTACION), 0, 10);

        var reputaciones = pagina.elementos().stream().map(Publicacion::reputacion).toList();
        assertTrue(reputaciones.stream().allMatch(r -> r >= 4.5), reputaciones.toString());
        assertEquals(reputaciones.stream().sorted(Comparator.reverseOrder()).toList(), reputaciones);
        assertTrue(pagina.total() < PUBLICADAS, "Tomás y Martín tienen 4.0");
    }

    @Test
    void ordenarPorMasRecienteMuestraPrimeroLaUltimaPublicada() {
        var borrador = c.publicaciones().listarPropias(MATEO).stream()
                .filter(p -> p.estado() == EstadoPublicacion.BORRADOR).findFirst().orElseThrow();
        demo.clock.adelantar(Duration.ofHours(1));
        c.publicaciones().cambiarEstado(MATEO, borrador.id(), EstadoPublicacion.PUBLICADA);

        var pagina = c.busqueda().buscar(new Criterios(null, null, null, null, null, null, null,
                Orden.MAS_RECIENTE), 0, 10);

        assertEquals("Consultas SQL desde cero", pagina.elementos().get(0).titulo());
        assertEquals(PUBLICADAS + 1, pagina.total());
    }

    @Test
    void buscarPaginaPorPagina() {
        var primera = c.busqueda().buscar(SIN_FILTROS, 0, 3);
        var ultima = c.busqueda().buscar(SIN_FILTROS, 2, 3);
        var fueraDeRango = c.busqueda().buscar(SIN_FILTROS, 3, 3);

        assertEquals(3, primera.elementos().size());
        assertEquals(PUBLICADAS, primera.total());
        assertEquals(0, primera.pagina());
        assertEquals(3, primera.tamanio());
        assertEquals(1, ultima.elementos().size());
        assertEquals(List.of(), fueraDeRango.elementos());
        assertEquals(PUBLICADAS, c.busqueda().buscar(null, 0, 20).total(), "sin criterios no filtra");
    }

    @Test
    void unaPaginaInvalidaSeRechazaEnCadaListado() {
        assertRechazada("El número de página no puede ser negativo: -1",
                () -> c.busqueda().buscar(SIN_FILTROS, -1, 10));
        assertRechazada("El tamaño de página tiene que ser al menos 1: 0",
                () -> c.contrataciones().listarContrataciones(MATEO, 0, 0));
        assertRechazada("El número de página no puede ser negativo: -2",
                () -> c.wallet().listarMovimientos(MATEO, -2, 5));
        assertRechazada("El tamaño de página tiene que ser al menos 1: -5",
                () -> c.notificaciones().listarNotificaciones(MATEO, 0, -5));
    }

    @Test
    void contratacionesPaginadas() {
        var todas = c.contrataciones().listarContrataciones(MATEO);

        var primera = c.contrataciones().listarContrataciones(MATEO, 0, 2);
        var segunda = c.contrataciones().listarContrataciones(MATEO, 1, 2);

        assertEquals(todas.size(), primera.total());
        assertEquals(todas.subList(0, 2), primera.elementos());
        assertEquals(todas.subList(2, 4), segunda.elementos());
    }

    @Test
    void movimientosPaginados() {
        var todos = c.wallet().listarMovimientos(MATEO);

        var primera = c.wallet().listarMovimientos(MATEO, 0, 2);

        assertEquals(todos.size(), primera.total());
        assertEquals(todos.subList(0, 2), primera.elementos());
        assertRechazada("No existe wallet con id user-nadie", () -> c.wallet().listarMovimientos("user-nadie", 0, 5));
    }

    @Test
    void notificacionesPaginadas() {
        var todas = c.notificaciones().listarNotificaciones(MATEO);

        var segunda = c.notificaciones().listarNotificaciones(MATEO, 1, 3);

        assertEquals(todas.size(), segunda.total());
        assertEquals(todas.subList(3, 6), segunda.elementos());
        assertEquals(0, c.notificaciones().listarNotificaciones("user-nadie", 0, 5).total(),
                "un usuario sin notificaciones tiene la bandeja vacía");
    }

    private static void assertRechazada(String mensaje, Executable operacion) {
        var error = assertThrows(OperacionRechazadaException.class, operacion);
        assertEquals(mensaje, error.getMessage());
    }
}
