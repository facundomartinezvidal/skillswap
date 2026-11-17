package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.model.Models.Publicacion;

import java.util.List;

/** Recomendaciones para el usuario. La estrategia la elige el armado del backend, no este controller. */
public interface RecomendacionController {

    List<Publicacion> recomendar(String userId);
}
