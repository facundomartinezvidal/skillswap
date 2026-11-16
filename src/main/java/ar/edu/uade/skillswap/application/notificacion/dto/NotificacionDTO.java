package ar.edu.uade.skillswap.application.notificacion.dto;

import java.time.Instant;

public record NotificacionDTO(String id, String tipo, String titulo, String detalle, Instant fecha, boolean leida) {
}
