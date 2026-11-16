package ar.edu.uade.skillswap.application.perfil;

import ar.edu.uade.skillswap.application.perfil.dto.DisponibilidadDTO;
import ar.edu.uade.skillswap.application.perfil.dto.PerfilDTO;
import ar.edu.uade.skillswap.application.publicacion.dto.PublicacionDTO;
import ar.edu.uade.skillswap.domain.perfil.DisponibilidadSemanal;
import ar.edu.uade.skillswap.domain.perfil.Perfil;

import java.util.List;

/** Pasa el perfil del dominio al DTO del perfil público. No tiene lógica. */
final class PerfilMapper {

    private PerfilMapper() {
    }

    static PerfilDTO aDTO(Perfil p, long contratacionesCompletadas, List<PublicacionDTO> publicacionesActivas) {
        return new PerfilDTO(p.userId(), p.tipo(), p.nombreVisible(), p.biografia(), p.zonaHoraria(),
                p.habilidades(), p.intereses(), p.disponibilidad().stream().map(PerfilMapper::aDTO).toList(),
                p.canal(), p.reputacion().promedio(), p.reputacion().cantidad(), contratacionesCompletadas,
                publicacionesActivas, p.estaActivo());
    }

    private static DisponibilidadDTO aDTO(DisponibilidadSemanal d) {
        return new DisponibilidadDTO(d.dia(), d.inicio(), d.fin());
    }
}
