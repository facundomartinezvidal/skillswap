package ar.edu.uade.skillswap.domain.contratacion.state;

public record Aceptada() implements EstadoContratacion {

    @Override
    public String nombre() {
        return "ACEPTADA";
    }

    @Override
    public EstadoContratacion pagar() {
        return new Confirmada();
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
