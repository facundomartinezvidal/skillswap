package ar.edu.uade.skillswap.dominio.publicacion;

public class CupoAgotadoException extends RuntimeException {

    public CupoAgotadoException(FranjaId franjaId, int cupo) {
        super("La franja " + franjaId.valor() + " ya tiene sus " + cupo + " lugares reservados");
    }
}
