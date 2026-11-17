package ar.edu.uade.skillswap.infrastructure.persistencia.contratacion;

import ar.edu.uade.skillswap.domain.shared.CambioEstado;
import ar.edu.uade.skillswap.infrastructure.persistencia.shared.CambioEstadoEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Tabla {@code historial_contrataciones}: un cambio de estado de una contratación. Nunca se modifica. */
@Entity
@Table(name = "historial_contrataciones")
public class HistorialContratacionEntity extends CambioEstadoEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contratacion_id")
    ContratacionEntity contratacion;

    protected HistorialContratacionEntity() {
    }

    HistorialContratacionEntity(ContratacionEntity contratacion, CambioEstado cambio) {
        super(cambio);
        this.contratacion = contratacion;
    }
}
