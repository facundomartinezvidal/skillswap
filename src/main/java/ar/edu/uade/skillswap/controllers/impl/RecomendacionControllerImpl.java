package ar.edu.uade.skillswap.controllers.impl;

import ar.edu.uade.skillswap.application.busqueda.BusquedaService;
import ar.edu.uade.skillswap.controllers.RecomendacionController;
import ar.edu.uade.skillswap.model.Models.Publicacion;

import java.util.List;
import java.util.Objects;

import static ar.edu.uade.skillswap.controllers.impl.ManejoDeErrores.ejecutar;

public class RecomendacionControllerImpl implements RecomendacionController {

    /** La pantalla de inicio muestra una fila de tres tarjetas. */
    static final int CANTIDAD_RECOMENDADAS = 3;

    private final BusquedaService busquedaService;

    public RecomendacionControllerImpl(BusquedaService busquedaService) {
        this.busquedaService = Objects.requireNonNull(busquedaService);
    }

    @Override
    public List<Publicacion> recomendar(String userId) {
        return ejecutar(() -> busquedaService.recomendar(userId, CANTIDAD_RECOMENDADAS).stream()
                .map(ModelosMapper::aModelo)
                .toList());
    }
}
