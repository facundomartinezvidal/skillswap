package ar.edu.uade.skillswap.infrastructure.persistencia;

import ar.edu.uade.skillswap.infrastructure.persistencia.contratacion.CalificacionEntity;
import ar.edu.uade.skillswap.infrastructure.persistencia.contratacion.ContratacionEntity;
import ar.edu.uade.skillswap.infrastructure.persistencia.contratacion.HistorialContratacionEntity;
import ar.edu.uade.skillswap.infrastructure.persistencia.notificacion.NotificacionEntity;
import ar.edu.uade.skillswap.infrastructure.persistencia.perfil.DisponibilidadEntity;
import ar.edu.uade.skillswap.infrastructure.persistencia.perfil.HistorialPerfilEntity;
import ar.edu.uade.skillswap.infrastructure.persistencia.perfil.PerfilEntity;
import ar.edu.uade.skillswap.infrastructure.persistencia.publicacion.FranjaEntity;
import ar.edu.uade.skillswap.infrastructure.persistencia.publicacion.HistorialPublicacionEntity;
import ar.edu.uade.skillswap.infrastructure.persistencia.publicacion.PromocionEntity;
import ar.edu.uade.skillswap.infrastructure.persistencia.publicacion.PublicacionEntity;
import ar.edu.uade.skillswap.infrastructure.persistencia.wallet.MovimientoEntity;
import ar.edu.uade.skillswap.infrastructure.persistencia.wallet.WalletEntity;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;

/**
 * Arma el {@link EntityManagerFactory} de Hibernate por código (JPA 3.2), sin {@code persistence.xml}:
 * así la lista de entidades y la configuración están en un solo lugar y se leen en Java.
 */
public final class Persistencia {

    private Persistencia() {
    }

    public static EntityManagerFactory crearEntityManagerFactory(ConfiguracionBaseDeDatos configuracion) {
        return new PersistenceConfiguration("skillswap")
                .managedClass(PerfilEntity.class)
                .managedClass(DisponibilidadEntity.class)
                .managedClass(HistorialPerfilEntity.class)
                .managedClass(PublicacionEntity.class)
                .managedClass(FranjaEntity.class)
                .managedClass(PromocionEntity.class)
                .managedClass(HistorialPublicacionEntity.class)
                .managedClass(ContratacionEntity.class)
                .managedClass(HistorialContratacionEntity.class)
                .managedClass(CalificacionEntity.class)
                .managedClass(WalletEntity.class)
                .managedClass(MovimientoEntity.class)
                .managedClass(NotificacionEntity.class)
                .property(PersistenceConfiguration.JDBC_URL, configuracion.url())
                .property(PersistenceConfiguration.JDBC_USER, configuracion.usuario())
                .property(PersistenceConfiguration.JDBC_PASSWORD, configuracion.password())
                // El esquema es de Flyway: Hibernate solo verifica que las entidades coincidan con las tablas.
                .property("hibernate.hbm2ddl.auto", "validate")
                // Los Instant del dominio se guardan como DATETIME en UTC, sin depender de la zona de la máquina.
                .property("hibernate.type.preferred_instant_jdbc_type", "TIMESTAMP")
                .property("hibernate.jdbc.time_zone", "UTC")
                .createEntityManagerFactory();
    }
}
