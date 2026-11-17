package ar.edu.uade.skillswap.infrastructure.persistencia;

import org.flywaydb.core.Flyway;

/**
 * Corre las migraciones de Flyway ({@code src/main/resources/db/migration}) al iniciar. El esquema lo
 * define solo Flyway: Hibernate nunca crea tablas, solo valida que las entidades coincidan con él.
 */
public final class Migraciones {

    private Migraciones() {
    }

    public static void aplicar(ConfiguracionBaseDeDatos configuracion) {
        Flyway.configure()
                .dataSource(configuracion.url(), configuracion.usuario(), configuracion.password())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}
