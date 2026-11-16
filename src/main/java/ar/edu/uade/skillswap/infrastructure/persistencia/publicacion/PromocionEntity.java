package ar.edu.uade.skillswap.infrastructure.persistencia.publicacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Tabla {@code promociones}. {@code tipo} es PORCENTUAL o FIJO, y {@code valor} es el porcentaje o el
 * monto. {@code orden} guarda en qué orden se aplican, porque el orden cambia el precio final.
 */
@Entity
@Table(name = "promociones")
public class PromocionEntity {

    static final String PORCENTUAL = "PORCENTUAL";
    static final String FIJO = "FIJO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id")
    PublicacionEntity publicacion;

    @Column(nullable = false)
    int orden;

    @Column(nullable = false)
    String tipo;

    @Column(nullable = false)
    long valor;

    protected PromocionEntity() {
    }

    PromocionEntity(PublicacionEntity publicacion, int orden) {
        this.publicacion = publicacion;
        this.orden = orden;
    }
}
