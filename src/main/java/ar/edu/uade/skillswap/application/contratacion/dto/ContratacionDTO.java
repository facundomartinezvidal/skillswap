package ar.edu.uade.skillswap.application.contratacion.dto;

import java.time.Instant;
import java.util.List;

/**
 * Contratación con su historial y comprobante. {@code propuesta} es nula si no hay una reprogramación
 * pendiente y {@code reintegro} es nulo si no se canceló.
 */
public record ContratacionDTO(String id, String publicacionId, String tituloPublicacion, String clienteId,
                              String proveedorId, String franjaId, Instant inicioSesion, long precioAcordado,
                              String estado, PropuestaReprogramacionDTO propuesta, ReintegroDTO reintegro,
                              List<CambioEstadoDTO> historial, List<CalificacionDTO> calificaciones) {
}
