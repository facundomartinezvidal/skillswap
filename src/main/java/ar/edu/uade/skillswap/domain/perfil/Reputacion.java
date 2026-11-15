package ar.edu.uade.skillswap.domain.perfil;

import ar.edu.uade.skillswap.domain.shared.Puntaje;

import java.util.Objects;

/**
 * Reputación acumulada a partir de calificaciones verificadas. Guarda la suma y la cantidad
 * para poder filtrar y ordenar sin recorrer las calificaciones. Es inmutable: registrar un
 * puntaje devuelve una reputación nueva.
 */
public record Reputacion(long sumaPuntajes, int cantidad) {

    public Reputacion {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad de calificaciones no puede ser negativa: " + cantidad);
        }
        if (sumaPuntajes < (long) cantidad * Puntaje.MINIMO || sumaPuntajes > (long) cantidad * Puntaje.MAXIMO) {
            throw new IllegalArgumentException(
                    "La suma " + sumaPuntajes + " no corresponde a " + cantidad + " calificaciones");
        }
    }

    public static Reputacion inicial() {
        return new Reputacion(0, 0);
    }

    public Reputacion registrar(Puntaje puntaje) {
        Objects.requireNonNull(puntaje, "El puntaje es obligatorio");
        return new Reputacion(sumaPuntajes + puntaje.valor(), cantidad + 1);
    }

    /** Promedio de los puntajes, o 0 sin calificaciones: usar {@link #cantidad()} para distinguir ese caso. */
    public double promedio() {
        return cantidad == 0 ? 0 : (double) sumaPuntajes / cantidad;
    }
}
