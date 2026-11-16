package ar.edu.uade.skillswap.infrastructure.identidad;

import ar.edu.uade.skillswap.application.perfil.ServicioIdentidad;

import java.util.Objects;
import java.util.Set;

/**
 * Servicio de identidad externo simulado (la consigna deja la autenticación fuera de alcance).
 * Implementa el puerto {@link ServicioIdentidad} con una lista fija de usuarios. Con un proveedor
 * real (por ejemplo OAuth), se reemplaza por un adapter que lo consulte, sin tocar la aplicación.
 */
public class ServicioIdentidadSimulado implements ServicioIdentidad {

    /** Usuarios de la demo; {@code user-mateo} es el usuario activo de la UI de la cátedra. */
    public static final Set<String> USUARIOS_DEMO = Set.of("user-mateo", "user-ana", "user-lucia", "user-acme");

    private final Set<String> usuarios;

    public ServicioIdentidadSimulado(Set<String> usuarios) {
        this.usuarios = Set.copyOf(Objects.requireNonNull(usuarios));
    }

    public static ServicioIdentidadSimulado conUsuariosDeDemo() {
        return new ServicioIdentidadSimulado(USUARIOS_DEMO);
    }

    @Override
    public boolean existe(String userId) {
        return userId != null && usuarios.contains(userId);
    }
}
