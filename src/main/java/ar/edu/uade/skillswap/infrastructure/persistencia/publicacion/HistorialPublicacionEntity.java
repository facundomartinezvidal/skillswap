package ar.edu.uade.skillswap.infrastructure.persistencia.publicacion;

import ar.edu.uade.skillswap.domain.shared.CambioEstado;
import ar.edu.uade.skillswap.infrastructure.persistencia.shared.CambioEstadoEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Tabla {@code historial_publicaciones}: un cambio de estado de una publicación. Nunca se modifica. */
@Entity
@Table(name = "historial_publicaciones")
public class HistorialPublicacionEntity extends CambioEstadoEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id")
    PublicacionEntity publicacion;

    protected HistorialPublicacionEntity() {
    }

    HistorialPublicacionEntity(PublicacionEntity publicacion, CambioEstado cambio) {
        super(cambio);
        this.publicacion = publicacion;
    }
}
