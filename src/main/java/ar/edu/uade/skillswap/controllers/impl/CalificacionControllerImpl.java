package ar.edu.uade.skillswap.controllers.impl;

import ar.edu.uade.skillswap.application.contratacion.ContratacionService;
import ar.edu.uade.skillswap.application.perfil.PerfilService;
import ar.edu.uade.skillswap.controllers.CalificacionController;
import ar.edu.uade.skillswap.model.Models.Perfil;

import java.util.Objects;

import static ar.edu.uade.skillswap.controllers.impl.ManejoDeErrores.ejecutar;

public class CalificacionControllerImpl implements CalificacionController {

    private final ContratacionService contratacionService;
    private final PerfilService perfilService;

    public CalificacionControllerImpl(ContratacionService contratacionService, PerfilService perfilService) {
        this.contratacionService = Objects.requireNonNull(contratacionService);
        this.perfilService = Objects.requireNonNull(perfilService);
    }

    @Override
    public Perfil calificar(String userId, String contratacionId, int puntaje, String comentario) {
        return ejecutar(() -> {
            var calificacion = contratacionService.calificar(userId, contratacionId, puntaje, comentario);
            return ModelosMapper.aModelo(perfilService.verPerfil(calificacion.destinatarioId()));
        });
    }
}
