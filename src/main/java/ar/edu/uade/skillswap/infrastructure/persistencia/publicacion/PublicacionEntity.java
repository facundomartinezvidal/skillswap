package ar.edu.uade.skillswap.infrastructure.persistencia.publicacion;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tabla {@code publicaciones}. Solo la usan {@link PublicacionMapper} y los repositorios: el dominio no
 * la conoce. Las colecciones son LAZY; los repositorios deciden qué traer con cada consulta.
 */
@Entity
@Table(name = "publicaciones")
public class PublicacionEntity {

    @Id
    String id;

    /** Orden de alta; lo completa la base (AUTO_INCREMENT) y solo se usa para ordenar en las consultas. */
    @Column(nullable = false, insertable = false, updatable = false)
    Long secuencia;

    @Column(name = "autor_id", nullable = false)
    String autorId;

    @Column(nullable = false)
    String tipo;

    String titulo;
    String descripcion;
    String categoria;
    String modalidad;

    @Column(name = "duracion_minutos")
    Integer duracionMinutos;

    @Column(nullable = false)
    int cupo;

    @Column(name = "precio_base")
    Long precioBase;

    /** Denormalizado: el {@code precioFinal()} del dominio al guardar, para filtrar y ordenar en SQL. */
    @Column(name = "precio_final")
    Long precioFinal;

    /** Denormalizado: la última vez que pasó a PUBLICADA, para ordenar por más reciente en SQL. */
    @Column(name = "publicada_en")
    Instant publicadaEn;

    @Column(nullable = false)
    String estado;

    @Column(name = "motivo_cancelacion")
    String motivoCancelacion;

    /** Bloqueo optimista del agregado entero, incluido el cupo de sus franjas (ver PublicacionRepositoryJpa). */
    @Version
    long version;

    @ElementCollection
    @CollectionTable(name = "publicacion_etiquetas", joinColumns = @JoinColumn(name = "publicacion_id"))
    @Column(name = "etiqueta", nullable = false)
    Set<String> etiquetas = new HashSet<>();

    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("inicio")
    List<FranjaEntity> franjas = new ArrayList<>();

    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    List<PromocionEntity> promociones = new ArrayList<>();

    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL)
    @OrderBy("id")
    List<HistorialPublicacionEntity> historial = new ArrayList<>();

    protected PublicacionEntity() {
    }

    PublicacionEntity(String id) {
        this.id = id;
    }
}
