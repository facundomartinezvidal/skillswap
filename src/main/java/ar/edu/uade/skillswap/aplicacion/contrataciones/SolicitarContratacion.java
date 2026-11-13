package ar.edu.uade.skillswap.aplicacion.contrataciones;

import ar.edu.uade.skillswap.aplicacion.puertos.RepositorioContrataciones;
import ar.edu.uade.skillswap.aplicacion.puertos.RepositorioPublicaciones;
import ar.edu.uade.skillswap.aplicacion.puertos.UnidadDeTrabajo;
import ar.edu.uade.skillswap.dominio.compartido.ReglaNegocioException;
import ar.edu.uade.skillswap.dominio.contrataciones.Contratacion;
import ar.edu.uade.skillswap.dominio.publicaciones.EstadoPublicacion;
import ar.edu.uade.skillswap.dominio.publicaciones.FranjaDisponibilidad;

import java.util.Objects;

public final class SolicitarContratacion {
    private final RepositorioPublicaciones publicaciones;
    private final RepositorioContrataciones contrataciones;
    private final UnidadDeTrabajo unidadDeTrabajo;

    public SolicitarContratacion(
            RepositorioPublicaciones publicaciones,
            RepositorioContrataciones contrataciones,
            UnidadDeTrabajo unidadDeTrabajo) {
        this.publicaciones = Objects.requireNonNull(publicaciones);
        this.contrataciones = Objects.requireNonNull(contrataciones);
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
    }

    public Contratacion ejecutar(Solicitud solicitud) {
        Objects.requireNonNull(solicitud);
        return unidadDeTrabajo.ejecutar(() -> {
            var publicacion = publicaciones.buscarPorId(solicitud.publicacionId())
                    .orElseThrow(() -> new ReglaNegocioException("La publicación no existe"));
            if (publicacion.estado() != EstadoPublicacion.PUBLICADA) {
                throw new ReglaNegocioException("La publicación no admite solicitudes");
            }
            if (!publicacion.disponibilidad().contains(solicitud.franjaElegida())) {
                throw new ReglaNegocioException("La franja elegida no está disponible en la publicación");
            }

            Contratacion contratacion = Contratacion.solicitar(
                    solicitud.id(), publicacion.id(), solicitud.clienteId(), publicacion.autorId(),
                    solicitud.mensaje(), solicitud.objetivo(), solicitud.franjaElegida(),
                    publicacion.precioSkillCoins());
            contrataciones.guardar(contratacion);
            return contratacion;
        });
    }

    public record Solicitud(
            String id,
            String publicacionId,
            String clienteId,
            String mensaje,
            String objetivo,
            FranjaDisponibilidad franjaElegida) {
        public Solicitud {
            Objects.requireNonNull(franjaElegida, "La franja elegida es obligatoria");
        }
    }
}
