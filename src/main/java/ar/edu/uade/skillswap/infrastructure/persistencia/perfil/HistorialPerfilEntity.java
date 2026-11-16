package ar.edu.uade.skillswap.infrastructure.persistencia.perfil;

import ar.edu.uade.skillswap.domain.shared.CambioEstado;
import ar.edu.uade.skillswap.infrastructure.persistencia.shared.CambioEstadoEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Tabla {@code historial_perfiles}: la baja lógica de un perfil (ACTIVO → INACTIVO). Nunca se modifica. */
@Entity
@Table(name = "historial_perfiles")
public class HistorialPerfilEntity extends CambioEstadoEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    PerfilEntity perfil;

    protected HistorialPerfilEntity() {
    }

    HistorialPerfilEntity(PerfilEntity perfil, CambioEstado cambio) {
        super(cambio);
        this.perfil = perfil;
    }
}
