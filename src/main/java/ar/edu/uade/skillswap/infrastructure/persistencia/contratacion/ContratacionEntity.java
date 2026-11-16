package ar.edu.uade.skillswap.infrastructure.persistencia.contratacion;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabla {@code contrataciones}. La solicitud, la propuesta de reprogramación y el reintegro son value
 * objects del dominio, así que van como columnas de esta tabla (las dos últimas, nulas si no hay).
 */
@Entity
@Table(name = "contrataciones")
public class ContratacionEntity {

    @Id
    String id;

    /** Orden de alta; lo completa la base (AUTO_INCREMENT) y solo se usa para ordenar los listados. */
    @Column(nullable = false, insertable = false, updatable = false)
    Long secuencia;

    @Column(name = "publicacion_id", nullable = false)
    String publicacionId;

    @Column(name = "cliente_id", nullable = false)
    String clienteId;

    @Column(name = "proveedor_id", nullable = false)
    String proveedorId;

    String mensaje;

    @Column(nullable = false)
    String objetivo;

    @Column(name = "franja_solicitada_id", nullable = false)
    String franjaSolicitadaId;

    @Column(name = "franja_id", nullable = false)
    String franjaId;

    @Column(name = "inicio_sesion", nullable = false)
    Instant inicioSesion;

    @Column(name = "precio_acordado", nullable = false)
    long precioAcordado;

    @Column(nullable = false)
    String estado;

    @Column(name = "propuesta_por")
    String propuestaPor;

    @Column(name = "propuesta_franja_id")
    String propuestaFranjaId;

    @Column(name = "propuesta_inicio")
    Instant propuestaInicio;

    @Column(name = "reintegro_cliente")
    Long reintegroCliente;

    @Column(name = "reintegro_proveedor")
    Long reintegroProveedor;

    @Version
    long version;

    @OneToMany(mappedBy = "contratacion", cascade = CascadeType.ALL)
    @OrderBy("id")
    List<HistorialContratacionEntity> historial = new ArrayList<>();

    @OneToMany(mappedBy = "contratacion", cascade = CascadeType.ALL)
    @OrderBy("fecha")
    List<CalificacionEntity> calificaciones = new ArrayList<>();

    protected ContratacionEntity() {
    }

    ContratacionEntity(String id) {
        this.id = id;
    }
}
