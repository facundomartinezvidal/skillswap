package ar.edu.uade.skillswap.application.publicacion.dto;

import ar.edu.uade.skillswap.domain.publicacion.Modalidad;

import java.time.Duration;
import java.util.Set;

/**
 * Contenido editable de una publicación. En borrador puede estar incompleto (por ejemplo, sin precio):
 * lo que falta se reclama al publicar.
 */
public record DatosPublicacionCommand(String titulo, String descripcion, String categoria, Modalidad modalidad,
                                      Duration duracionEstimada, int cupo, Long precioBase, Set<String> etiquetas) {
}
