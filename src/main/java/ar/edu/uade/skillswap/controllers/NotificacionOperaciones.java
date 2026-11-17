package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.model.Models.Notificacion;

/** Bandeja de notificaciones paginada, de la más reciente a la más vieja. */
public interface NotificacionOperaciones {

    PaginaVista<Notificacion> listarNotificaciones(String userId, int pagina, int tamanio);
}
