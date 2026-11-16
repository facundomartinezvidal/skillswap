package ar.edu.uade.skillswap.application.publicacion.dto;

import ar.edu.uade.skillswap.domain.publicacion.Modalidad;
import ar.edu.uade.skillswap.domain.publicacion.TipoPublicacion;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Publicación tal como la muestran la búsqueda, el detalle y el panel del autor.
 * {@code precioBase} y {@code precioFinal} son nulos mientras el borrador no tiene precio.
 * El estado viaja como texto ({@code "PUBLICADA"}), igual que los enums de {@code Models}.
 */
public record PublicacionDTO(String id, String autorId, String autorNombre, double reputacionAutor,
                             TipoPublicacion tipo, String titulo, String descripcion, String categoria,
                             Modalidad modalidad, Duration duracionEstimada, int cupo, Long precioBase,
                             Long precioFinal, Set<String> etiquetas, String estado, String motivoCancelacion,
                             List<FranjaDTO> franjas, List<PromocionDTO> promociones) {
}
