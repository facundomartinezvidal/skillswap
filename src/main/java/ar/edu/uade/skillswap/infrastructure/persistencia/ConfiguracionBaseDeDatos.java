package ar.edu.uade.skillswap.infrastructure.persistencia;

import java.util.Objects;

/**
 * Cómo conectarse a MySQL. Se lee de las variables de entorno {@code DB_URL}, {@code DB_USER} y
 * {@code DB_PASSWORD}; si no están, usa los valores de desarrollo del {@code docker-compose.yml}
 * (puerto 3307, porque el 3306 suele estar ocupado por un MySQL instalado en la máquina).
 */
public record ConfiguracionBaseDeDatos(String url, String usuario, String password) {

    public static final String URL_POR_DEFECTO = "jdbc:mysql://localhost:3307/skillswap";
    public static final String USUARIO_POR_DEFECTO = "skillswap";
    public static final String PASSWORD_POR_DEFECTO = "skillswap";

    public ConfiguracionBaseDeDatos {
        Objects.requireNonNull(url, "La URL de la base es obligatoria");
        Objects.requireNonNull(usuario, "El usuario de la base es obligatorio");
        Objects.requireNonNull(password, "La contraseña de la base es obligatoria");
    }

    public static ConfiguracionBaseDeDatos desdeEntorno() {
        return new ConfiguracionBaseDeDatos(
                variable("DB_URL", URL_POR_DEFECTO),
                variable("DB_USER", USUARIO_POR_DEFECTO),
                variable("DB_PASSWORD", PASSWORD_POR_DEFECTO));
    }

    private static String variable(String nombre, String porDefecto) {
        var valor = System.getenv(nombre);
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }
}
