package ar.edu.uade.skillswap.infrastructure.persistencia.shared;

import ar.edu.uade.skillswap.domain.shared.CambioEstado;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.time.Instant;

/**
 * Columnas comunes de las tres tablas de historial (publicación, contratación y perfil). Cada fila es un
 * {@link CambioEstado} del dominio. El id autoincremental conserva el orden en que se agregaron.
 */
@MappedSuperclass
public abstract class CambioEstadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String desde;

    @Column(nullable = false)
    private String hacia;

    @Column(nullable = false)
    private Instant fecha;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    private String motivo;

    protected CambioEstadoEntity() {
    }

    protected CambioEstadoEntity(CambioEstado cambio) {
        this.desde = cambio.desde();
        this.hacia = cambio.hacia();
        this.fecha = cambio.fecha();
        this.actorId = cambio.actorId();
        this.motivo = cambio.motivo();
    }

    public CambioEstado aDominio() {
        return new CambioEstado(desde, hacia, fecha, actorId, motivo);
    }
}
