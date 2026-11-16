package ar.edu.uade.skillswap.domain.publicacion.state;

public record Borrador() implements EstadoPublicacion {

    @Override
    public String nombre() {
        return "BORRADOR";
    }

    @Override
    public EstadoPublicacion publicar() {
        return new Publicada();
    }

    @Override
    public EstadoPublicacion cancelar() {
        return new Cancelada();
    }

    @Override
    public void editar() {
        // En borrador se puede editar libremente.
    }

    @Override
    public boolean admitePromociones() {
        return true;
    }

    @Override
    public boolean esVisibleParaTodos() {
        return false;
    }
}
