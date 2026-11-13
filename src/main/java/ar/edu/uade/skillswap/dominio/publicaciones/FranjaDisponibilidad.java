package ar.edu.uade.skillswap.dominio.publicaciones;

import java.time.LocalDateTime;
import java.util.Objects;

public record FranjaDisponibilidad(LocalDateTime inicio, LocalDateTime fin) {
    public FranjaDisponibilidad {
        Objects.requireNonNull(inicio, "El inicio de la franja es obligatorio");
        Objects.requireNonNull(fin, "El fin de la franja es obligatorio");
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("El inicio debe ser anterior al fin");
        }
    }
}
