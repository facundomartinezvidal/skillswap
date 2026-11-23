package ar.edu.uade.skillswap.domain.contratacion.state;

/** Estado final: no admite ninguna acción. El cliente puede volver a solicitar, con otra contratación. */
public record Rechazada() implements EstadoContratacion {

    @Override
    public boolean admiteNuevaSolicitud() {
        return true;
    }

    @Override
    public String nombre() {
        return "RECHAZADA";
    }
}
