package ar.edu.uade.skillswap.infrastructure.notificacion;

import java.util.ArrayList;
import java.util.List;

/**
 * Simula el SDK de un servicio de notificaciones push externo. Habla de "tokens de dispositivo" y
 * de un mensaje de una sola línea, no de usuarios ni de título y detalle: por eso hace falta un adapter.
 * Guarda los mensajes "enviados" para poder verlos en los tests y en la demo.
 */
public class ProveedorPushSimulado {

    /** El mensaje tal como lo pide este proveedor. */
    public record MensajePush(String tokenDispositivo, String texto) {
    }

    private final List<MensajePush> enviados = new ArrayList<>();

    public void notificar(String tokenDispositivo, String texto) {
        enviados.add(new MensajePush(tokenDispositivo, texto));
        System.out.println("[PUSH] a " + tokenDispositivo + ": " + texto);
    }

    public List<MensajePush> enviados() {
        return List.copyOf(enviados);
    }
}
