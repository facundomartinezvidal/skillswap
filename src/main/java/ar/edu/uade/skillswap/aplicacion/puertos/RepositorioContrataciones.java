package ar.edu.uade.skillswap.aplicacion.puertos;

import ar.edu.uade.skillswap.dominio.contrataciones.Contratacion;

import java.util.Optional;

public interface RepositorioContrataciones {
    void guardar(Contratacion contratacion);
    Optional<Contratacion> buscarPorId(String contratacionId);

    /**
     * Persiste la aceptación solo si hay cupo. La implementación debe comprobar y
     * reservar de forma atómica; false indica que otra operación consumió el cupo.
     */
    boolean guardarAceptacionSiHayCupo(Contratacion contratacionAceptada, int cupo);
}
