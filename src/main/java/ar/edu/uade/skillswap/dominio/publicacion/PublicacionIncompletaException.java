package ar.edu.uade.skillswap.dominio.publicacion;

import java.util.List;

public class PublicacionIncompletaException extends RuntimeException {

    private final List<String> faltantes;

    public PublicacionIncompletaException(List<String> faltantes) {
        super("No se puede publicar. Falta: " + String.join(", ", faltantes));
        this.faltantes = List.copyOf(faltantes);
    }

    public List<String> faltantes() {
        return faltantes;
    }
}
