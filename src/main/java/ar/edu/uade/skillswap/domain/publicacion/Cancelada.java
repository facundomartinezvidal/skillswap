package ar.edu.uade.skillswap.domain.publicacion;

/** Estado final: no admite ninguna acción. */
public record Cancelada() implements EstadoPublicacion {

    @Override
    public String nombre() {
        return "CANCELADA";
    }
}
