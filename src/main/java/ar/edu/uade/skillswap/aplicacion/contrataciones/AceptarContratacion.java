package ar.edu.uade.skillswap.aplicacion.contrataciones;

import ar.edu.uade.skillswap.aplicacion.puertos.RepositorioContrataciones;
import ar.edu.uade.skillswap.aplicacion.puertos.RepositorioPublicaciones;
import ar.edu.uade.skillswap.aplicacion.puertos.UnidadDeTrabajo;
import ar.edu.uade.skillswap.dominio.compartido.ReglaNegocioException;

import java.time.LocalDateTime;
import java.util.Objects;

public final class AceptarContratacion {
    private final RepositorioPublicaciones publicaciones;
    private final RepositorioContrataciones contrataciones;
    private final UnidadDeTrabajo unidadDeTrabajo;

    public AceptarContratacion(
            RepositorioPublicaciones publicaciones,
            RepositorioContrataciones contrataciones,
            UnidadDeTrabajo unidadDeTrabajo) {
        this.publicaciones = Objects.requireNonNull(publicaciones);
        this.contrataciones = Objects.requireNonNull(contrataciones);
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
    }

    public void ejecutar(String contratacionId, String actorId, LocalDateTime fecha) {
        unidadDeTrabajo.ejecutar(() -> {
            var contratacion = contrataciones.buscarPorId(contratacionId)
                    .orElseThrow(() -> new ReglaNegocioException("La contratación no existe"));
            var publicacion = publicaciones.buscarPorId(contratacion.publicacionId())
                    .orElseThrow(() -> new ReglaNegocioException("La publicación no existe"));
            var aceptada = contratacion.aceptar(actorId, fecha);
            if (!contrataciones.guardarAceptacionSiHayCupo(aceptada, publicacion.cupo())) {
                throw new ReglaNegocioException("No hay cupos disponibles para la franja elegida");
            }
            return null;
        });
    }
}
