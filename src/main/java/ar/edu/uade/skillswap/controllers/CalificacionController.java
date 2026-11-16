package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.model.Models.Perfil;

/** Calificación verificada de una contratación completada. */
public interface CalificacionController {

    /** Califica a la otra parte y devuelve su perfil con la reputación ya recalculada. */
    Perfil calificar(String userId, String contratacionId, int puntaje, String comentario);
}
