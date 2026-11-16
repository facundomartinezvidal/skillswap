package ar.edu.uade.skillswap.application.perfil.dto;

import java.time.ZoneId;
import java.util.Set;

/** Datos editables del perfil, en una sola pantalla. La biografía es opcional. */
public record EditarPerfilCommand(String nombreVisible, String biografia, ZoneId zonaHoraria,
                                  Set<String> habilidades, Set<String> intereses) {
}
