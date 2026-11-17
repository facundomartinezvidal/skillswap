package ar.edu.uade.skillswap.infrastructure.persistencia.contratacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** Tabla {@code calificaciones}. La base rechaza una segunda calificación del mismo autor en la contratación. */
@Entity
@Table(name = "calificaciones")
public class CalificacionEntity {

    @Id
    String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contratacion_id")
    ContratacionEntity contratacion;

    @Column(name = "autor_id", nullable = false)
    String autorId;

    @Column(name = "destinatario_id", nullable = false)
    String destinatarioId;

    @Column(nullable = false)
    int puntaje;

    String comentario;

    @Column(nullable = false)
    Instant fecha;

    protected CalificacionEntity() {
    }

    CalificacionEntity(String id, ContratacionEntity contratacion) {
        this.id = id;
        this.contratacion = contratacion;
    }
}
