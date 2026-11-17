package ar.edu.uade.skillswap.infrastructure.persistencia;

import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.domain.publicacion.Modalidad;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.publicacion.TipoPublicacion;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.infrastructure.persistencia.contratacion.ContratacionRepositoryJpa;
import ar.edu.uade.skillswap.infrastructure.persistencia.notificacion.NotificacionRepositoryJpa;
import ar.edu.uade.skillswap.infrastructure.persistencia.perfil.PerfilRepositoryJpa;
import ar.edu.uade.skillswap.infrastructure.persistencia.publicacion.PublicacionRepositoryJpa;
import ar.edu.uade.skillswap.infrastructure.persistencia.wallet.WalletRepositoryJpa;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * Base de los tests de persistencia: un MySQL 8 real en Docker (Testcontainers), uno por clase de test,
 * con el esquema creado por las migraciones de Flyway (igual que en la aplicación). Antes de cada test
 * vacía las tablas, así cada test arranca de cero. Si no hay Docker, los tests quedan deshabilitados.
 */
@Testcontainers(disabledWithoutDocker = true)
abstract class BaseDeDatosDePrueba {

    protected static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");

    /** En orden: primero las tablas que referencian a otras (igual se desactivan las FK al vaciar). */
    private static final List<String> TABLAS = List.of("notificaciones", "movimientos_wallet", "wallets",
            "calificaciones", "historial_contrataciones", "contrataciones", "historial_publicaciones",
            "promociones", "franjas_disponibilidad", "publicacion_etiquetas", "publicaciones", "historial_perfiles",
            "perfil_disponibilidad", "perfil_intereses", "perfil_habilidades", "perfiles");

    @Container
    protected static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0")
            .withDatabaseName("skillswap")
            .withUsername("skillswap")
            .withPassword("skillswap");

    protected static ConfiguracionBaseDeDatos configuracion;
    protected static EntityManagerFactory entityManagerFactory;
    protected static UnidadDeTrabajoJpa unidadDeTrabajo;
    protected static PerfilRepositoryJpa perfiles;
    protected static PublicacionRepositoryJpa publicaciones;
    protected static ContratacionRepositoryJpa contrataciones;
    protected static WalletRepositoryJpa wallets;
    protected static NotificacionRepositoryJpa notificaciones;

    @BeforeAll
    static void crearEsquema() {
        configuracion = new ConfiguracionBaseDeDatos(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        Migraciones.aplicar(configuracion);
        entityManagerFactory = Persistencia.crearEntityManagerFactory(configuracion);
        unidadDeTrabajo = new UnidadDeTrabajoJpa(entityManagerFactory);
        perfiles = new PerfilRepositoryJpa(unidadDeTrabajo);
        publicaciones = new PublicacionRepositoryJpa(unidadDeTrabajo);
        contrataciones = new ContratacionRepositoryJpa(unidadDeTrabajo);
        wallets = new WalletRepositoryJpa(unidadDeTrabajo);
        notificaciones = new NotificacionRepositoryJpa(unidadDeTrabajo);
    }

    @AfterAll
    static void cerrar() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @BeforeEach
    void vaciarTablas() throws SQLException {
        try (var conexion = DriverManager.getConnection(configuracion.url(), configuracion.usuario(),
                configuracion.password()); var sentencia = conexion.createStatement()) {
            sentencia.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (var tabla : TABLAS) {
                sentencia.execute("TRUNCATE TABLE " + tabla);
            }
            sentencia.execute("SET FOREIGN_KEY_CHECKS = 1");
            // La garantía la crea la migración: se vuelve a crear porque se vació la tabla.
            sentencia.execute("INSERT INTO wallets (titular_id, saldo, version) VALUES ('skillswap-garantia', 0, 0)");
        }
    }

    /** Cuenta filas con SQL directo, para verificar lo que quedó en la base sin pasar por los repositorios. */
    protected static long contarFilas(String sql) throws SQLException {
        try (var conexion = DriverManager.getConnection(configuracion.url(), configuracion.usuario(),
                configuracion.password()); var resultado = conexion.createStatement().executeQuery(sql)) {
            resultado.next();
            return resultado.getLong(1);
        }
    }

    protected static Perfil guardarPerfil(String userId) {
        var perfil = Perfil.crear(userId, TipoPerfil.PROFESIONAL, "Perfil " + userId, ZoneId.of("UTC"));
        perfiles.save(perfil);
        return perfil;
    }

    /** Una publicación PUBLICADA en {@code publicadaEn}, con una franja que empieza en 3 días. */
    protected static Publicacion guardarPublicada(String autorId, String titulo, String categoria, Modalidad modalidad,
                                                  long precio, Set<String> etiquetas, Instant publicadaEn) {
        var publicacion = Publicacion.crearBorrador(autorId, TipoPublicacion.OFERTA);
        publicacion.editar(titulo, "Descripción de " + titulo, categoria, modalidad, Duration.ofMinutes(60), 2,
                new SkillCoins(precio), etiquetas);
        publicacion.agregarFranja(AHORA.plus(Duration.ofDays(3)), AHORA.plus(Duration.ofDays(3)).plusSeconds(3600));
        publicacion.publicar(autorId, publicadaEn);
        publicaciones.save(publicacion);
        return publicacion;
    }
}
