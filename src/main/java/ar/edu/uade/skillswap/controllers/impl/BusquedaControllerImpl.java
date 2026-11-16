package ar.edu.uade.skillswap.controllers.impl;

import ar.edu.uade.skillswap.application.busqueda.BusquedaService;
import ar.edu.uade.skillswap.application.busqueda.CriteriosBusqueda;
import ar.edu.uade.skillswap.application.busqueda.OrdenBusqueda;
import ar.edu.uade.skillswap.controllers.RecomendacionController;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models.FiltrosBusqueda;
import ar.edu.uade.skillswap.model.Models.Publicacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static ar.edu.uade.skillswap.controllers.impl.ManejoDeErrores.ejecutar;

public class BusquedaControllerImpl implements UiControllers.BusquedaController {

    /** Opción del combo de categorías de la UI que significa "sin filtrar". */
    static final String TODAS = "Todas";

    private final BusquedaService busquedaService;
    private final RecomendacionController recomendaciones;

    public BusquedaControllerImpl(BusquedaService busquedaService, RecomendacionController recomendaciones) {
        this.busquedaService = Objects.requireNonNull(busquedaService);
        this.recomendaciones = Objects.requireNonNull(recomendaciones);
    }

    @Override
    public List<Publicacion> buscar(FiltrosBusqueda filtros) {
        return ejecutar(() -> busquedaService.buscar(aCriterios(filtros), ModelosMapper.PRIMERA_PAGINA)
                .elementos().stream()
                .map(ModelosMapper::aModelo)
                .toList());
    }

    /** La pantalla de inicio pide las recomendaciones a la búsqueda; las resuelve el RecomendacionController. */
    @Override
    public List<Publicacion> recomendar(String userId) {
        return recomendaciones.recomendar(userId);
    }

    @Override
    public List<String> categorias() {
        return ejecutar(() -> {
            var categorias = new ArrayList<String>();
            categorias.add(TODAS);
            categorias.addAll(busquedaService.categorias());
            return List.copyOf(categorias);
        });
    }

    /**
     * La UI solo filtra por texto, categoría, modalidad y precio máximo; el resto de los criterios queda
     * sin filtrar. Un texto vacío, "Todas" o un precio máximo de 0 tampoco filtran.
     */
    static CriteriosBusqueda aCriterios(FiltrosBusqueda f) {
        var texto = f.texto() == null || f.texto().isBlank() ? null : f.texto().trim();
        var categoria = f.categoria() == null || f.categoria().isBlank() || TODAS.equals(f.categoria())
                ? null : f.categoria();
        var precioMaximo = f.precioMaximo() > 0 ? Long.valueOf(f.precioMaximo()) : null;
        return new CriteriosBusqueda(texto, categoria, ModelosMapper.aDominio(f.modalidad()), null, precioMaximo,
                null, null, aOrden(f.orden()));
    }

    /** Los textos del combo de orden de la UI. Cualquier otro valor ordena por relevancia. */
    static OrdenBusqueda aOrden(String orden) {
        if ("Mejor reputación".equals(orden)) {
            return OrdenBusqueda.MEJOR_REPUTACION;
        }
        if ("Menor precio".equals(orden)) {
            return OrdenBusqueda.MENOR_PRECIO;
        }
        if ("Más reciente".equals(orden)) {
            return OrdenBusqueda.MAS_RECIENTE;
        }
        return OrdenBusqueda.RELEVANCIA;
    }
}
