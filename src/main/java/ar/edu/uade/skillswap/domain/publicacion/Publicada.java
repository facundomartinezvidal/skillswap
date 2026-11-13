package ar.edu.uade.skillswap.domain.publicacion;

public record Publicada() implements EstadoPublicacion {

    @Override
    public String nombre() {
        return "PUBLICADA";
    }

    @Override
    public EstadoPublicacion pausar() {
        return new Pausada();
    }

    @Override
    public EstadoPublicacion cerrar() {
        return new Cerrada();
    }

    @Override
    public EstadoPublicacion cancelar() {
        return new Cancelada();
    }

    @Override
    public boolean admiteSolicitudes() {
        return true;
    }
}
