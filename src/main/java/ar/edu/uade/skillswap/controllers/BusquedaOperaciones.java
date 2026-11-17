package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.model.Models.Modalidad;
import ar.edu.uade.skillswap.model.Models.Publicacion;

/**
 * Búsqueda completa y paginada que pide la consigna. La UI de la cátedra solo filtra por texto,
 * categoría, modalidad y precio máximo, y no pagina.
 */
public interface BusquedaOperaciones {

    enum Orden { RELEVANCIA, MEJOR_REPUTACION, MENOR_PRECIO, MAS_RECIENTE }

    /** Cualquier criterio nulo no filtra; sin orden, se ordena por relevancia. */
    record Criterios(String texto, String categoria, Modalidad modalidad, Integer precioMinimo,
                     Integer precioMaximo, String habilidad, Double reputacionMinima, Orden orden) {
    }

    PaginaVista<Publicacion> buscar(Criterios criterios, int pagina, int tamanio);
}
