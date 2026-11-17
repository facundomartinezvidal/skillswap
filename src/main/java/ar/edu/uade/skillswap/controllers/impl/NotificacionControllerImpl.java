package ar.edu.uade.skillswap.controllers.impl;

import ar.edu.uade.skillswap.application.notificacion.NotificacionService;
import ar.edu.uade.skillswap.controllers.NotificacionOperaciones;
import ar.edu.uade.skillswap.controllers.PaginaVista;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.model.Models.Notificacion;

import java.util.List;
import java.util.Objects;

import static ar.edu.uade.skillswap.controllers.impl.ManejoDeErrores.ejecutar;
import static ar.edu.uade.skillswap.controllers.impl.ManejoDeErrores.ejecutarSinResultado;

public class NotificacionControllerImpl implements UiControllers.NotificacionController, NotificacionOperaciones {

    private final NotificacionService notificacionService;

    public NotificacionControllerImpl(NotificacionService notificacionService) {
        this.notificacionService = Objects.requireNonNull(notificacionService);
    }

    /** La UI no pagina: ve la primera página. */
    @Override
    public List<Notificacion> listarNotificaciones(String userId) {
        return listarNotificaciones(userId, 0, ModelosMapper.TAMANIO_UI).elementos();
    }

    @Override
    public PaginaVista<Notificacion> listarNotificaciones(String userId, int pagina, int tamanio) {
        return ejecutar(() -> ModelosMapper.aVista(
                notificacionService.listar(userId, new PaginaSolicitada(pagina, tamanio)), ModelosMapper::aModelo));
    }

    @Override
    public void marcarLeida(String userId, String notificacionId) {
        ejecutarSinResultado(() -> notificacionService.marcarLeida(userId, notificacionId));
    }

    @Override
    public void marcarTodasLeidas(String userId) {
        ejecutarSinResultado(() -> notificacionService.marcarTodasLeidas(userId));
    }
}
