package ar.edu.uade.skillswap.domain.shared;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Una página de un listado: sus elementos, qué página es y cuántos elementos hay en total.
 * La consigna pide paginar todos los listados de consulta.
 */
public record Pagina<T>(List<T> elementos, int numero, int tamanio, long total) {

    public Pagina {
        Objects.requireNonNull(elementos, "Los elementos de la página son obligatorios");
        elementos = List.copyOf(elementos);
        if (numero < 0 || tamanio < 1 || total < 0) {
            throw new IllegalArgumentException("Página inválida: número " + numero + ", tamaño " + tamanio
                    + ", total " + total);
        }
    }

    /** Corta la página pedida de un listado completo. Lo usan los repositorios en memoria. */
    public static <T> Pagina<T> de(List<T> todos, PaginaSolicitada pagina) {
        Objects.requireNonNull(todos, "El listado es obligatorio");
        Objects.requireNonNull(pagina, "La página solicitada es obligatoria");
        int desde = Math.min(pagina.desde(), todos.size());
        int hasta = Math.min(desde + pagina.tamanio(), todos.size());
        return new Pagina<>(todos.subList(desde, hasta), pagina.numero(), pagina.tamanio(), todos.size());
    }

    /** Misma página con cada elemento transformado, por ejemplo de entidad a DTO. */
    public <R> Pagina<R> map(Function<T, R> transformacion) {
        return new Pagina<>(elementos.stream().map(transformacion).toList(), numero, tamanio, total);
    }

    public int totalPaginas() {
        return (int) ((total + tamanio - 1) / tamanio);
    }
}
