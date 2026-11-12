package ar.edu.uade.skillswap.dominio.publicacion;

public record Pausada() implements EstadoPublicacion {

    @Override
    public String nombre() {
        return "PAUSADA";
    }

    /** Decisión D2: una publicación pausada puede volver a publicarse. */
    @Override
    public EstadoPublicacion reanudar() {
        return new Publicada();
    }

    @Override
    public EstadoPublicacion cerrar() {
        return new Cerrada();
    }

    @Override
    public EstadoPublicacion cancelar() {
        return new Cancelada();
    }
}
