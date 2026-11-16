package ar.edu.uade.skillswap.infrastructure.persistencia.contratacion;

import ar.edu.uade.skillswap.domain.contratacion.Calificacion;
import ar.edu.uade.skillswap.domain.contratacion.CalificacionId;
import ar.edu.uade.skillswap.domain.contratacion.Contratacion;
import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.contratacion.PropuestaReprogramacion;
import ar.edu.uade.skillswap.domain.contratacion.Reintegro;
import ar.edu.uade.skillswap.domain.contratacion.Solicitud;
import ar.edu.uade.skillswap.domain.contratacion.state.Aceptada;
import ar.edu.uade.skillswap.domain.contratacion.state.Cancelada;
import ar.edu.uade.skillswap.domain.contratacion.state.Completada;
import ar.edu.uade.skillswap.domain.contratacion.state.Confirmada;
import ar.edu.uade.skillswap.domain.contratacion.state.EnCurso;
import ar.edu.uade.skillswap.domain.contratacion.state.EstadoContratacion;
import ar.edu.uade.skillswap.domain.contratacion.state.Pendiente;
import ar.edu.uade.skillswap.domain.contratacion.state.Rechazada;
import ar.edu.uade.skillswap.domain.publicacion.FranjaId;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.shared.Puntaje;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.infrastructure.persistencia.shared.CambioEstadoEntity;

import java.util.UUID;

/** Convierte entre {@link Contratacion} (dominio) y {@link ContratacionEntity} (tabla), en los dos sentidos. */
final class ContratacionMapper {

    private ContratacionMapper() {
    }

    /** Arma el agregado con {@code reconstruir}. Las colecciones de la entidad ya tienen que estar cargadas. */
    static Contratacion aDominio(ContratacionEntity e) {
        var propuesta = e.propuestaPor == null ? null
                : new PropuestaReprogramacion(e.propuestaPor, franja(e.propuestaFranjaId), e.propuestaInicio);
        var reintegro = e.reintegroCliente == null ? null
                : new Reintegro(new SkillCoins(e.reintegroCliente), new SkillCoins(e.reintegroProveedor));
        return Contratacion.reconstruir(
                new ContratacionId(UUID.fromString(e.id)),
                new PublicacionId(UUID.fromString(e.publicacionId)),
                e.clienteId,
                e.proveedorId,
                new Solicitud(e.mensaje, e.objetivo, franja(e.franjaSolicitadaId)),
                franja(e.franjaId),
                e.inicioSesion,
                new SkillCoins(e.precioAcordado),
                estado(e.estado),
                propuesta,
                reintegro,
                e.historial.stream().map(CambioEstadoEntity::aDominio).toList(),
                e.calificaciones.stream().map(ContratacionMapper::calificacion).toList());
    }

    /** Copia el estado del agregado en la entidad. Del historial y las calificaciones solo agrega lo nuevo. */
    static void copiar(Contratacion c, ContratacionEntity e) {
        e.publicacionId = c.publicacionId().valor().toString();
        e.clienteId = c.clienteId();
        e.proveedorId = c.proveedorId();
        e.mensaje = c.solicitud().mensaje();
        e.objetivo = c.solicitud().objetivo();
        e.franjaSolicitadaId = c.solicitud().franjaElegida().valor().toString();
        e.franjaId = c.franjaId().valor().toString();
        e.inicioSesion = c.inicioSesion();
        e.precioAcordado = c.precioAcordado().valor();
        e.estado = c.estado().nombre();
        var propuesta = c.propuesta();
        e.propuestaPor = propuesta == null ? null : propuesta.propuestaPor();
        e.propuestaFranjaId = propuesta == null ? null : propuesta.franjaId().valor().toString();
        e.propuestaInicio = propuesta == null ? null : propuesta.inicio();
        var reintegro = c.reintegro();
        e.reintegroCliente = reintegro == null ? null : reintegro.alCliente().valor();
        e.reintegroProveedor = reintegro == null ? null : reintegro.alProveedor().valor();

        var historial = c.historial();
        for (int i = e.historial.size(); i < historial.size(); i++) {
            e.historial.add(new HistorialContratacionEntity(e, historial.get(i)));
        }
        for (var calificacion : c.calificaciones()) {
            var id = calificacion.id().valor().toString();
            if (e.calificaciones.stream().noneMatch(guardada -> guardada.id.equals(id))) {
                var fila = new CalificacionEntity(id, e);
                fila.autorId = calificacion.autorId();
                fila.destinatarioId = calificacion.destinatarioId();
                fila.puntaje = calificacion.puntaje().valor();
                fila.comentario = calificacion.comentario();
                fila.fecha = calificacion.fecha();
                e.calificaciones.add(fila);
            }
        }
    }

    static EstadoContratacion estado(String nombre) {
        return switch (nombre) {
            case "PENDIENTE" -> new Pendiente();
            case "ACEPTADA" -> new Aceptada();
            case "CONFIRMADA" -> new Confirmada();
            case "EN_CURSO" -> new EnCurso();
            case "COMPLETADA" -> new Completada();
            case "RECHAZADA" -> new Rechazada();
            case "CANCELADA" -> new Cancelada();
            default -> throw new IllegalStateException("Estado de contratación desconocido: " + nombre);
        };
    }

    private static Calificacion calificacion(CalificacionEntity fila) {
        return new Calificacion(new CalificacionId(UUID.fromString(fila.id)), fila.autorId, fila.destinatarioId,
                new Puntaje(fila.puntaje), fila.comentario, fila.fecha);
    }

    private static FranjaId franja(String id) {
        return new FranjaId(UUID.fromString(id));
    }
}
