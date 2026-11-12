package ar.edu.uade.skillswap.dominio.publicacion;

/** Estado final: no admite ninguna acción. */
public record Cancelada() implements EstadoPublicacion {

    @Override
    public String nombre() {
        return "CANCELADA";
    }
}
