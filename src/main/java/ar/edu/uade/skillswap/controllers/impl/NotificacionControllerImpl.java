package ar.edu.uade.skillswap.controllers.impl;

import ar.edu.uade.skillswap.application.notificacion.NotificacionService;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models.Notificacion;

import java.util.List;
import java.util.Objects;

import static ar.edu.uade.skillswap.controllers.impl.ManejoDeErrores.ejecutar;
import static ar.edu.uade.skillswap.controllers.impl.ManejoDeErrores.ejecutarSinResultado;

public class NotificacionControllerImpl implements UiControllers.NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionControllerImpl(NotificacionService notificacionService) {
        this.notificacionService = Objects.requireNonNull(notificacionService);
    }

    @Override
    public List<Notificacion> listarNotificaciones(String userId) {
        return ejecutar(() -> notificacionService.listar(userId, ModelosMapper.PRIMERA_PAGINA)
                .elementos().stream()
                .map(ModelosMapper::aModelo)
                .toList());
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
