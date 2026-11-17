package ar.edu.uade.skillswap.infrastructure.persistencia.perfil;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Tabla {@code perfiles}, identificada por el userId (no hay un id de perfil aparte). */
@Entity
@Table(name = "perfiles")
public class PerfilEntity {

    @Id
    @Column(name = "user_id")
    String userId;

    @Column(nullable = false)
    String tipo;

    @Column(name = "nombre_visible", nullable = false)
    String nombreVisible;

    @Column(nullable = false)
    String biografia;

    @Column(name = "zona_horaria", nullable = false)
    String zonaHoraria;

    @Column(name = "canal_notificacion", nullable = false)
    String canal;

    @Column(name = "reputacion_suma", nullable = false)
    long reputacionSuma;

    @Column(name = "reputacion_cantidad", nullable = false)
    int reputacionCantidad;

    @Column(nullable = false)
    boolean activo;

    /** Dos calificaciones simultáneas al mismo perfil no pueden pisarse la reputación. */
    @Version
    long version;

    @ElementCollection
    @CollectionTable(name = "perfil_habilidades", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "habilidad", nullable = false)
    Set<String> habilidades = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "perfil_intereses", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "interes", nullable = false)
    Set<String> intereses = new HashSet<>();

    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    List<DisponibilidadEntity> disponibilidad = new ArrayList<>();

    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL)
    @OrderBy("id")
    List<HistorialPerfilEntity> historial = new ArrayList<>();

    protected PerfilEntity() {
    }

    PerfilEntity(String userId) {
        this.userId = userId;
    }
}
