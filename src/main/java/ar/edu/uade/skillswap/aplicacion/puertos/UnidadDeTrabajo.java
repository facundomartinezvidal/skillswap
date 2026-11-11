package ar.edu.uade.skillswap.aplicacion.puertos;

import java.util.function.Supplier;

public interface UnidadDeTrabajo {
    <T> T ejecutar(Supplier<T> operacion);
}
