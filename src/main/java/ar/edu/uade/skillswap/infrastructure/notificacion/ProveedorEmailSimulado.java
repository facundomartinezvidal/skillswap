package ar.edu.uade.skillswap.infrastructure.notificacion;

import java.util.ArrayList;
import java.util.List;

/**
 * Simula el SDK de un proveedor de email externo (la consigna pide no usar proveedores reales).
 * Tiene su propia API, distinta de la que espera la aplicación: por eso hace falta un adapter.
 * Guarda los correos "enviados" para poder verlos en los tests y en la demo.
 */
public class ProveedorEmailSimulado {

    /** El correo tal como lo pide este proveedor. */
    public record Correo(String para, String asunto, String cuerpo) {
    }

    private final List<Correo> enviados = new ArrayList<>();

    public void enviarCorreo(Correo correo) {
        enviados.add(correo);
        System.out.println("[EMAIL] para " + correo.para() + ": " + correo.asunto());
    }

    public List<Correo> enviados() {
        return List.copyOf(enviados);
    }
}
