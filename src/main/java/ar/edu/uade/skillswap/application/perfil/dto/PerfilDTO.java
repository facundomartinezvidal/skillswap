package ar.edu.uade.skillswap.application.perfil.dto;

import ar.edu.uade.skillswap.application.publicacion.dto.PublicacionDTO;
import ar.edu.uade.skillswap.domain.perfil.CanalNotificacion;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;

import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * Perfil público: datos, reputación (promedio y cantidad de calificaciones), publicaciones activas
 * e historial resumido (contrataciones completadas).
 */
public record PerfilDTO(String userId, TipoPerfil tipo, String nombreVisible, String biografia, ZoneId zonaHoraria,
                        Set<String> habilidades, Set<String> intereses, List<DisponibilidadDTO> disponibilidad,
                        CanalNotificacion canal, double reputacion, int cantidadCalificaciones,
                        long contratacionesCompletadas, List<PublicacionDTO> publicacionesActivas, boolean activo) {

    public PerfilDTO {
        habilidades = Set.copyOf(habilidades);
        intereses = Set.copyOf(intereses);
        disponibilidad = List.copyOf(disponibilidad);
        publicacionesActivas = List.copyOf(publicacionesActivas);
    }
}
