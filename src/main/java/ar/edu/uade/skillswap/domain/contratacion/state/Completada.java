package ar.edu.uade.skillswap.domain.contratacion.state;

/** Estado final: no admite ninguna transición. Es el único estado que admite calificar. */
public record Completada() implements EstadoContratacion {

    @Override
    public String nombre() {
        return "COMPLETADA";
    }

    @Override
    public boolean admiteCalificacion() {
        return true;
    }

    @Override
    public boolean ocupaCupo() {
        return true;
    }
}
