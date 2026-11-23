package ar.edu.uade.skillswap.controllers;

import java.time.LocalDateTime;

/**
 * Una franja de una publicación para la vista: cuándo es, cuántos lugares le quedan y si todavía se puede
 * solicitar (no empezó y tiene lugar). {@code Models} es de la cátedra, por eso este record va acá.
 */
public record FranjaVista(String id, LocalDateTime inicio, LocalDateTime fin, int lugaresLibres, boolean disponible) {
}
