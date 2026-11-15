package ar.edu.uade.skillswap.domain.perfil;

import ar.edu.uade.skillswap.domain.perfil.exception.DisponibilidadInvalidaException;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Franja horaria que se repite cada semana, en la zona horaria del perfil.
 * Los intervalos son semiabiertos [inicio, fin): dos franjas contiguas no se superponen.
 */
public record DisponibilidadSemanal(DayOfWeek dia, LocalTime inicio, LocalTime fin) {

    public DisponibilidadSemanal {
        if (dia == null || inicio == null || fin == null) {
            throw new DisponibilidadInvalidaException("La disponibilidad necesita día, inicio y fin");
        }
        if (!inicio.isBefore(fin)) {
            throw new DisponibilidadInvalidaException("El inicio de la disponibilidad tiene que ser anterior al fin");
        }
    }

    public boolean seSolapaCon(DisponibilidadSemanal otra) {
        return dia == otra.dia && inicio.isBefore(otra.fin) && otra.inicio.isBefore(fin);
    }
}
