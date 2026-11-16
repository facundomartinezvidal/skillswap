package ar.edu.uade.skillswap.application.contratacion.dto;

import java.time.Instant;

public record CalificacionDTO(String autorId, String destinatarioId, int puntaje, String comentario, Instant fecha) {
}
