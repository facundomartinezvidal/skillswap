package ar.edu.uade.skillswap.domain.publicacion.exception;

import ar.edu.uade.skillswap.domain.shared.DomainException;

import java.util.List;

public class PublicacionIncompletaException extends DomainException {

    private final List<String> faltantes;

    public PublicacionIncompletaException(List<String> faltantes) {
        super("No se puede publicar. Falta: " + String.join(", ", faltantes));
        this.faltantes = List.copyOf(faltantes);
    }

    public List<String> faltantes() {
        return faltantes;
    }
}
