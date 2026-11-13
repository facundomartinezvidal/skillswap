package ar.edu.uade.skillswap.domain.publicacion;

/** Estado final: no admite ninguna acción. */
public record Cerrada() implements EstadoPublicacion {

    @Override
    public String nombre() {
        return "CERRADA";
    }
}
