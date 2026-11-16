package ar.edu.uade.skillswap.domain.shared;

/**
 * Qué página de un listado se pide. El número arranca en 0, igual que en JPA
 * ({@code setFirstResult(desde())}, {@code setMaxResults(tamanio())}).
 */
public record PaginaSolicitada(int numero, int tamanio) {

    public PaginaSolicitada {
        if (numero < 0) {
            throw new IllegalArgumentException("El número de página no puede ser negativo: " + numero);
        }
        if (tamanio < 1) {
            throw new IllegalArgumentException("El tamaño de página tiene que ser al menos 1: " + tamanio);
        }
    }

    public static PaginaSolicitada primera(int tamanio) {
        return new PaginaSolicitada(0, tamanio);
    }

    /** Posición del primer elemento de la página dentro del listado completo. */
    public int desde() {
        return numero * tamanio;
    }
}
