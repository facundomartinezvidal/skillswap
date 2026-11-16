package ar.edu.uade.skillswap.infrastructure.persistencia.publicacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** Tabla {@code franjas_disponibilidad}: una franja de una publicación, con sus lugares reservados. */
@Entity
@Table(name = "franjas_disponibilidad")
public class FranjaEntity {

    @Id
    String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id")
    PublicacionEntity publicacion;

    @Column(nullable = false)
    Instant inicio;

    @Column(nullable = false)
    Instant fin;

    @Column(nullable = false)
    int reservados;

    protected FranjaEntity() {
    }

    FranjaEntity(String id, PublicacionEntity publicacion, Instant inicio, Instant fin) {
        this.id = id;
        this.publicacion = publicacion;
        this.inicio = inicio;
        this.fin = fin;
    }
}
