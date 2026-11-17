package ar.edu.uade.skillswap.controllers.impl;

import ar.edu.uade.skillswap.application.perfil.PerfilService;
import ar.edu.uade.skillswap.application.perfil.dto.CrearPerfilCommand;
import ar.edu.uade.skillswap.application.perfil.dto.DisponibilidadDTO;
import ar.edu.uade.skillswap.application.perfil.dto.EditarPerfilCommand;
import ar.edu.uade.skillswap.application.perfil.dto.PerfilDTO;
import ar.edu.uade.skillswap.controllers.DisponibilidadVista;
import ar.edu.uade.skillswap.controllers.PerfilOperaciones;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.domain.perfil.CanalNotificacion;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.model.Models.Perfil;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static ar.edu.uade.skillswap.controllers.impl.ManejoDeErrores.ejecutar;
import static ar.edu.uade.skillswap.controllers.impl.ManejoDeErrores.ejecutarSinResultado;

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
    public Perfil editarPerfil(String userId, String nombreVisible, String biografia, ZoneId zonaHoraria,
                               Set<String> habilidades, Set<String> intereses) {
        return ejecutar(() -> ModelosMapper.aModelo(perfilService.editarPerfil(userId,
                new EditarPerfilCommand(nombreVisible, biografia, zonaHoraria, habilidades, intereses))));
    }

    @Override
    public Perfil elegirCanal(String userId, CanalNotificacion canal) {
        return ejecutar(() -> ModelosMapper.aModelo(perfilService.elegirCanal(userId, canal)));
    }

    @Override
    public List<DisponibilidadVista> verDisponibilidad(String userId) {
        return ejecutar(() -> aVista(perfilService.verPerfil(userId)));
    }

    @Override
    public List<DisponibilidadVista> agregarDisponibilidad(String userId, DayOfWeek dia, LocalTime inicio,
                                                           LocalTime fin) {
        return ejecutar(() -> aVista(perfilService.agregarDisponibilidad(userId,
                new DisponibilidadDTO(dia, inicio, fin))));
    }

    @Override
    public List<DisponibilidadVista> quitarDisponibilidad(String userId, DayOfWeek dia, LocalTime inicio,
                                                          LocalTime fin) {
        return ejecutar(() -> aVista(perfilService.quitarDisponibilidad(userId,
                new DisponibilidadDTO(dia, inicio, fin))));
    }

    @Override
    public void darDeBaja(String userId, String motivo) {
        ejecutarSinResultado(() -> perfilService.darDeBaja(userId, motivo));
    }

    private static List<DisponibilidadVista> aVista(PerfilDTO perfil) {
        return perfil.disponibilidad().stream()
                .map(d -> new DisponibilidadVista(d.dia(), d.inicio(), d.fin()))
                .toList();
    }
}
