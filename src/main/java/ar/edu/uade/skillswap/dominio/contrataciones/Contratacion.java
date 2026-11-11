package ar.edu.uade.skillswap.dominio.contrataciones;

import ar.edu.uade.skillswap.dominio.compartido.ReglaNegocioException;
import ar.edu.uade.skillswap.dominio.publicaciones.FranjaDisponibilidad;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Contratacion {
    private final String id;
    private final String publicacionId;
    private final String clienteId;
    private final String proveedorId;
    private final String mensaje;
    private final String objetivo;
    private final FranjaDisponibilidad franjaElegida;
    private final long precioSkillCoins;
    private final EstadoContratacion estado;
    private final List<HistorialEstadoContratacion> historial;

    private Contratacion(
            String id,
            String publicacionId,
            String clienteId,
            String proveedorId,
            String mensaje,
            String objetivo,
            FranjaDisponibilidad franjaElegida,
            long precioSkillCoins,
            EstadoContratacion estado,
            List<HistorialEstadoContratacion> historial) {
        this.id = textoObligatorio(id, "El id de la contratación es obligatorio");
        this.publicacionId = textoObligatorio(publicacionId, "La publicación es obligatoria");
        this.clienteId = textoObligatorio(clienteId, "El cliente es obligatorio");
        this.proveedorId = textoObligatorio(proveedorId, "El proveedor es obligatorio");
        if (this.clienteId.equals(this.proveedorId)) {
            throw new ReglaNegocioException("El autor no puede solicitar su propia publicación");
        }
        this.mensaje = mensaje == null ? "" : mensaje.trim();
        this.objetivo = textoObligatorio(objetivo, "El objetivo es obligatorio");
        this.franjaElegida = Objects.requireNonNull(franjaElegida, "La franja elegida es obligatoria");
        if (precioSkillCoins < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        this.precioSkillCoins = precioSkillCoins;
        this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
        this.historial = List.copyOf(Objects.requireNonNull(historial, "El historial es obligatorio"));
    }

    public static Contratacion solicitar(
            String id,
            String publicacionId,
            String clienteId,
            String proveedorId,
            String mensaje,
            String objetivo,
            FranjaDisponibilidad franjaElegida,
            long precioSkillCoins) {
        return new Contratacion(id, publicacionId, clienteId, proveedorId, mensaje, objetivo,
                franjaElegida, precioSkillCoins, EstadoContratacion.PENDIENTE, List.of());
    }

    public Contratacion aceptar(String actorId, LocalDateTime fecha) {
        exigirActor(actorId, proveedorId, "Solo el proveedor puede aceptar la solicitud");
        return transicionar(EstadoContratacion.ACEPTADA, actorId, "", fecha);
    }

    public Contratacion rechazar(String actorId, String motivo, LocalDateTime fecha) {
        exigirActor(actorId, proveedorId, "Solo el proveedor puede rechazar la solicitud");
        return transicionar(EstadoContratacion.RECHAZADA, actorId, motivo, fecha);
    }

    public Contratacion confirmarPago(String actorId, LocalDateTime fecha) {
        exigirActor(actorId, clienteId, "Solo el cliente puede confirmar el pago");
        return transicionar(EstadoContratacion.CONFIRMADA, actorId, "Pago confirmado", fecha);
    }

    public Contratacion iniciar(String actorId, LocalDateTime fecha) {
        exigirActor(actorId, proveedorId, "Solo el proveedor puede iniciar la sesión");
        return transicionar(EstadoContratacion.EN_CURSO, actorId, "Sesión iniciada", fecha);
    }

    public Contratacion completar(String actorId, LocalDateTime fecha) {
        exigirActor(actorId, proveedorId, "Solo el proveedor puede completar la sesión");
        return transicionar(EstadoContratacion.COMPLETADA, actorId, "Sesión completada", fecha);
    }

    public Contratacion cancelar(String actorId, String motivo, LocalDateTime fecha) {
        if (!esParticipante(actorId)) {
            throw new ReglaNegocioException("Solo una de las partes puede cancelar la contratación");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("La cancelación requiere un motivo");
        }
        return transicionar(EstadoContratacion.CANCELADA, actorId, motivo, fecha);
    }

    private Contratacion transicionar(
            EstadoContratacion destino,
            String actorId,
            String motivo,
            LocalDateTime fecha) {
        Objects.requireNonNull(fecha, "La fecha es obligatoria");
        if (!transicionPermitida(estado, destino)) {
            throw new ReglaNegocioException("Transición inválida de " + estado + " a " + destino);
        }
        List<HistorialEstadoContratacion> nuevoHistorial = new ArrayList<>(historial);
        nuevoHistorial.add(new HistorialEstadoContratacion(
                estado, destino, fecha, actorId, motivo));
        return new Contratacion(id, publicacionId, clienteId, proveedorId, mensaje, objetivo,
                franjaElegida, precioSkillCoins, destino, nuevoHistorial);
    }

    private boolean transicionPermitida(EstadoContratacion actual, EstadoContratacion destino) {
        return switch (actual) {
            case PENDIENTE -> destino == EstadoContratacion.ACEPTADA
                    || destino == EstadoContratacion.RECHAZADA;
            case ACEPTADA -> destino == EstadoContratacion.CONFIRMADA || destino == EstadoContratacion.CANCELADA;
            case CONFIRMADA -> destino == EstadoContratacion.EN_CURSO || destino == EstadoContratacion.CANCELADA;
            case EN_CURSO -> destino == EstadoContratacion.COMPLETADA;
            case COMPLETADA, RECHAZADA, CANCELADA -> false;
        };
    }

    private void exigirActor(String actorId, String esperado, String mensaje) {
        if (!esperado.equals(actorId)) {
            throw new ReglaNegocioException(mensaje);
        }
    }

    private boolean esParticipante(String actorId) {
        return clienteId.equals(actorId) || proveedorId.equals(actorId);
    }

    private static String textoObligatorio(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.trim();
    }

    public String id() { return id; }
    public String publicacionId() { return publicacionId; }
    public String clienteId() { return clienteId; }
    public String proveedorId() { return proveedorId; }
    public String mensaje() { return mensaje; }
    public String objetivo() { return objetivo; }
    public FranjaDisponibilidad franjaElegida() { return franjaElegida; }
    public long precioSkillCoins() { return precioSkillCoins; }
    public EstadoContratacion estado() { return estado; }
    public List<HistorialEstadoContratacion> historial() { return historial; }
}
