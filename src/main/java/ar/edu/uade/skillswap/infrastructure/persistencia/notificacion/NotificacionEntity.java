package ar.edu.uade.skillswap.infrastructure.persistencia.notificacion;

import ar.edu.uade.skillswap.domain.notificacion.Notificacion;
import ar.edu.uade.skillswap.domain.notificacion.NotificacionId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Tabla {@code notificaciones}: la bandeja de cada usuario. Solo cambia el estado de leída. */
@Entity
@Table(name = "notificaciones")
public class NotificacionEntity {

    @Id
    String id;

    /** Orden de alta; desempata notificaciones con la misma fecha. Lo completa la base. */
    @Column(nullable = false, insertable = false, updatable = false)
    Long secuencia;

    @Column(name = "destinatario_id", nullable = false)
    String destinatarioId;

    @Column(nullable = false)
    String tipo;

    @Column(nullable = false)
    String titulo;

    @Column(nullable = false)
    String detalle;

    @Column(nullable = false)
    Instant fecha;

    @Column(nullable = false)
    boolean leida;

    protected NotificacionEntity() {
    }

    NotificacionEntity(Notificacion n) {
        this.id = n.id().valor().toString();
        this.destinatarioId = n.destinatarioId();
        this.tipo = n.tipo();
        this.titulo = n.titulo();
        this.detalle = n.detalle();
        this.fecha = n.fecha();
    }

    Notificacion aDominio() {
        return Notificacion.reconstruir(new NotificacionId(UUID.fromString(id)), destinatarioId, tipo, titulo,
                detalle, fecha, leida);
    }
}
