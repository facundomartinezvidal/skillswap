package ar.edu.uade.skillswap.domain.publicacion;

import ar.edu.uade.skillswap.domain.publicacion.exception.CupoAgotadoException;
import ar.edu.uade.skillswap.domain.publicacion.exception.FranjaInvalidaException;

import java.time.Instant;
import java.util.Objects;

/**
 * Franja horaria de una publicación. Solo la {@link Publicacion} modifica las reservas,
 * por eso {@code reservar} y {@code liberar} no son públicos.
 */
public final class FranjaDisponibilidad {

    private final FranjaId id;
    private final Instant inicio;
    private final Instant fin;
    private int reservados;

    FranjaDisponibilidad(FranjaId id, Instant inicio, Instant fin) {
        this.id = Objects.requireNonNull(id, "El id de la franja es obligatorio");
        if (inicio == null || fin == null) {
            throw new FranjaInvalidaException("La franja necesita inicio y fin");
        }
        if (!inicio.isBefore(fin)) {
            throw new FranjaInvalidaException("El inicio de la franja tiene que ser anterior al fin");
        }
        this.inicio = inicio;
        this.fin = fin;
        this.reservados = 0;
    }

    /** Los intervalos son semiabiertos [inicio, fin): dos franjas contiguas no se solapan. */
    boolean seSolapaCon(FranjaDisponibilidad otra) {
        return inicio.isBefore(otra.fin) && otra.inicio.isBefore(fin);
    }

    void reservar(int cupo) {
        if (reservados >= cupo) {
            throw new CupoAgotadoException(id, cupo);
        }
        reservados++;
    }

    void liberar() {
        if (reservados > 0) {
            reservados--;
        }
    }

    public boolean estaOcupada(int cupo) {
        return reservados >= cupo;
    }

    public FranjaId id() {
        return id;
    }

    public Instant inicio() {
        return inicio;
    }

    public Instant fin() {
        return fin;
    }

    public int reservados() {
        return reservados;
    }
}
