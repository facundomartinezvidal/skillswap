package ar.edu.uade.skillswap.dominio.publicacion;

/** Estado final: no admite ninguna acción. */
public record Cerrada() implements EstadoPublicacion {

    @Override
    public String nombre() {
        return "CERRADA";
    }
}
