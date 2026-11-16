package ar.edu.uade.skillswap.application.publicacion.dto;

import java.time.Instant;

/** Franja de disponibilidad con los lugares que le quedan libres. */
public record FranjaDTO(String id, Instant inicio, Instant fin, int lugaresLibres) {
}
