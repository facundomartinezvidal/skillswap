package ar.edu.uade.skillswap.domain.shared;

import java.time.Instant;
import java.util.List;

/**
 * Algo relevante que pasó en un agregado. Cada evento declara a quiénes les interesa y el texto
 * que ven, para que otros módulos (por ejemplo, notificación) lo usen sin conocer al agregado.
 */
public interface EventoDominio {

    /** Nombre estable del evento, por ejemplo {@code "SOLICITUD_RECIBIDA"}. */
    String tipo();

    /** Ids de los usuarios a los que hay que notificar. */
    List<String> interesados();

    String titulo();

    String detalle();

    Instant ocurridoEn();
}
