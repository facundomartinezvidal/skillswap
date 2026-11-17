package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.model.Models.Publicacion;

import java.time.LocalDateTime;

/** Datos de la publicación que la UI de la cátedra no carga: franjas y promociones. */
public interface PublicacionOperaciones {

    Publicacion agregarFranja(String userId, String publicacionId, LocalDateTime inicio, LocalDateTime fin);

    Publicacion agregarDescuentoPorcentual(String userId, String publicacionId, int porcentaje);

    Publicacion agregarDescuentoFijo(String userId, String publicacionId, int skillCoins);
}
