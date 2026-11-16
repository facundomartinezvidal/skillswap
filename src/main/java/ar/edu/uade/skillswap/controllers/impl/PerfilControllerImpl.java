package ar.edu.uade.skillswap.controllers.impl;

import ar.edu.uade.skillswap.application.perfil.PerfilService;
import ar.edu.uade.skillswap.application.perfil.dto.CrearPerfilCommand;
import ar.edu.uade.skillswap.controllers.PerfilOperaciones;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.domain.perfil.CanalNotificacion;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.model.Models.Perfil;

import java.util.Objects;

import static ar.edu.uade.skillswap.controllers.impl.ManejoDeErrores.ejecutar;

public class PerfilControllerImpl implements UiControllers.PerfilController, PerfilOperaciones {

    private final PerfilService perfilService;

    public PerfilControllerImpl(PerfilService perfilService) {
        this.perfilService = Objects.requireNonNull(perfilService);
    }

    @Override
    public Perfil verPerfil(String userId) {
        return ejecutar(() -> ModelosMapper.aModelo(perfilService.verPerfil(userId)));
    }

    /** La zona horaria se toma de la máquina, igual que las fechas que muestra la UI. */
    @Override
    public Perfil crearPerfil(String userId, TipoPerfil tipo, String nombreVisible) {
        return ejecutar(() -> ModelosMapper.aModelo(perfilService.crearPerfil(
                new CrearPerfilCommand(userId, tipo, nombreVisible, ModelosMapper.ZONA))));
    }

    @Override
    public Perfil elegirCanal(String userId, CanalNotificacion canal) {
        return ejecutar(() -> ModelosMapper.aModelo(perfilService.elegirCanal(userId, canal)));
    }
}
