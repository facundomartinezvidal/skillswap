package ar.edu.uade.skillswap.domain.contratacion.state;

public record Pendiente() implements EstadoContratacion {

    @Override
    public String nombre() {
        return "PENDIENTE";
    }

    @Override
    public EstadoContratacion aceptar() {
        return new Aceptada();
    }

    @Override
    public EstadoContratacion rechazar() {
        return new Rechazada();
    }

    /** El cliente retira su solicitud antes de que el proveedor responda. */
    @Override
    public EstadoContratacion cancelar() {
        return new Cancelada();
    }
}
