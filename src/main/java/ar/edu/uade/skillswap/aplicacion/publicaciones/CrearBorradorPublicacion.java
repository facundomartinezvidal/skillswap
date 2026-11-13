package ar.edu.uade.skillswap.aplicacion.publicaciones;

import ar.edu.uade.skillswap.aplicacion.puertos.RepositorioPublicaciones;
import ar.edu.uade.skillswap.aplicacion.puertos.UnidadDeTrabajo;
import ar.edu.uade.skillswap.dominio.publicaciones.Modalidad;
import ar.edu.uade.skillswap.dominio.publicaciones.Publicacion;
import ar.edu.uade.skillswap.dominio.publicaciones.TipoPublicacion;

import java.util.List;
import java.util.Objects;

public final class CrearBorradorPublicacion {
    private final RepositorioPublicaciones publicaciones;
    private final UnidadDeTrabajo unidadDeTrabajo;

    public CrearBorradorPublicacion(
            RepositorioPublicaciones publicaciones,
            UnidadDeTrabajo unidadDeTrabajo) {
        this.publicaciones = Objects.requireNonNull(publicaciones);
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
    }

    public Publicacion ejecutar(Solicitud solicitud) {
        Objects.requireNonNull(solicitud);
        return unidadDeTrabajo.ejecutar(() -> {
            Publicacion publicacion = Publicacion.crearBorrador(
                    solicitud.id(), solicitud.autorId(), solicitud.tipo(), solicitud.titulo(),
                    solicitud.descripcion(), solicitud.categoria(), solicitud.modalidad(),
                    solicitud.duracionMinutos(), solicitud.cupo(), solicitud.precioSkillCoins(),
                    solicitud.etiquetas());
            publicaciones.guardar(publicacion);
            return publicacion;
        });
    }

    public record Solicitud(
            String id,
            String autorId,
            TipoPublicacion tipo,
            String titulo,
            String descripcion,
            String categoria,
            Modalidad modalidad,
            int duracionMinutos,
            int cupo,
            long precioSkillCoins,
            List<String> etiquetas) {
        public Solicitud {
            etiquetas = List.copyOf(Objects.requireNonNull(etiquetas));
        }
    }
}
