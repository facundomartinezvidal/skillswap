package ar.edu.uade.skillswap.controllers;

import java.util.List;

/** Una página de un listado para la vista: sus elementos, qué página es (desde 0), el tamaño y el total. */
public record PaginaVista<T>(List<T> elementos, int pagina, int tamanio, long total) {

    public PaginaVista {
        elementos = List.copyOf(elementos);
    }
}
