package ar.edu.uade.skillswap.domain.contratacion.state;

public record EnCurso() implements EstadoContratacion {

    @Override
    public String nombre() {
        return "EN_CURSO";
    }

    @Override
    public EstadoContratacion completar() {
        return new Completada();
    }
}
