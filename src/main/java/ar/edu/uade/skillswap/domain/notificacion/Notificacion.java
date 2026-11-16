package ar.edu.uade.skillswap.domain.notificacion;

import ar.edu.uade.skillswap.domain.notificacion.exception.NotificacionAjenaException;
import ar.edu.uade.skillswap.domain.shared.EventoDominio;

import java.time.Instant;
import java.util.Objects;

/**
 * Raíz del agregado Notificacion: un aviso en la bandeja de un usuario. Es genérica, se crea a
 * partir de cualquier {@link EventoDominio} y no conoce al agregado que lo emitió. Es inmutable
 * salvo el estado de leída, que solo cambia su destinatario.
 */
public final class Notificacion {

    private final NotificacionId id;
    private final String destinatarioId;
    private final String tipo;
    private final String titulo;
    private final String detalle;
    private final Instant fecha;
    private boolean leida;

    private Notificacion(NotificacionId id, String destinatarioId, String tipo, String titulo, String detalle,
                         Instant fecha) {
        this.id = id;
        this.destinatarioId = destinatarioId;
        this.tipo = tipo;
        this.titulo = titulo;
        this.detalle = detalle;
        this.fecha = fecha;
    }

    /** Crea la notificación sin leer. El destinatario tiene que ser uno de los interesados del evento. */
    public static Notificacion para(String destinatarioId, EventoDominio evento) {
        if (destinatarioId == null || destinatarioId.isBlank()) {
            throw new IllegalArgumentException("El destinatario es obligatorio");
        }
        Objects.requireNonNull(evento, "El evento es obligatorio");
        if (!evento.interesados().contains(destinatarioId)) {
            throw new IllegalArgumentException(
                    "El usuario " + destinatarioId + " no es interesado en el evento " + evento.tipo());
        }
        return new Notificacion(NotificacionId.generate(), destinatarioId,
                Objects.requireNonNull(evento.tipo(), "El tipo del evento es obligatorio"),
                Objects.requireNonNull(evento.titulo(), "El título del evento es obligatorio"),
                Objects.requireNonNull(evento.detalle(), "El detalle del evento es obligatorio"),
                Objects.requireNonNull(evento.ocurridoEn(), "La fecha del evento es obligatoria"));
    }

    /** Vuelve a armar una notificación guardada, leída o no. Lo usan los repositorios (JPA o en memoria). */
    public static Notificacion reconstruir(NotificacionId id, String destinatarioId, String tipo, String titulo,
                                           String detalle, Instant fecha, boolean leida) {
        Objects.requireNonNull(id, "El id de la notificación es obligatorio");
        Objects.requireNonNull(destinatarioId, "El destinatario es obligatorio");
        Objects.requireNonNull(tipo, "El tipo es obligatorio");
        Objects.requireNonNull(titulo, "El título es obligatorio");
        Objects.requireNonNull(detalle, "El detalle es obligatorio");
        Objects.requireNonNull(fecha, "La fecha es obligatoria");
        var notificacion = new Notificacion(id, destinatarioId, tipo, titulo, detalle, fecha);
        notificacion.leida = leida;
        return notificacion;
    }

    /** Solo el destinatario puede marcarla. Marcarla de nuevo no cambia nada. */
    public void marcarLeida(String actorId) {
        if (!destinatarioId.equals(actorId)) {
            throw new NotificacionAjenaException(actorId, id);
        }
        this.leida = true;
    }

    public boolean estaLeida() {
        return leida;
    }

    public NotificacionId id() {
        return id;
    }

    public String destinatarioId() {
        return destinatarioId;
    }

    public String tipo() {
        return tipo;
    }

    public String titulo() {
        return titulo;
    }

    public String detalle() {
        return detalle;
    }

    public Instant fecha() {
        return fecha;
    }
}
