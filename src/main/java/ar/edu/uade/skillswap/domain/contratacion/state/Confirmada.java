package ar.edu.uade.skillswap.domain.contratacion.state;

/** Pagada: el reintegro de una cancelación depende de la política. */
public record Confirmada() implements EstadoContratacion {

    @Override
    public String nombre() {
        return "CONFIRMADA";
    }

    @Override
    public EstadoContratacion iniciar() {
        return new EnCurso();
    }

    @Override
    public EstadoContratacion cancelar() {
        return new Cancelada();
    }

    @Override
    public boolean admiteReprogramacion() {
        return true;
    }
}
