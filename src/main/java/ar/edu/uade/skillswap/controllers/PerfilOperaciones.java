package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.domain.perfil.CanalNotificacion;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.model.Models.Perfil;

/** Alta del perfil y preferencia de canal, que la UI de la cátedra no tiene. */
public interface PerfilOperaciones {

    Perfil crearPerfil(String userId, TipoPerfil tipo, String nombreVisible);

    Perfil elegirCanal(String userId, CanalNotificacion canal);
}
