package ar.edu.uade.skillswap.domain.contratacion.state;

/** Estado final: no admite ninguna acción. */
public record Rechazada() implements EstadoContratacion {

    @Override
    public String nombre() {
        return "RECHAZADA";
    }
}
