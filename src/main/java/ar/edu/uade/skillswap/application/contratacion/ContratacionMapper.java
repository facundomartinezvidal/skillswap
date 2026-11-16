package ar.edu.uade.skillswap.application.contratacion;

import ar.edu.uade.skillswap.application.contratacion.dto.CalificacionDTO;
import ar.edu.uade.skillswap.application.contratacion.dto.CambioEstadoDTO;
import ar.edu.uade.skillswap.application.contratacion.dto.ContratacionDTO;
import ar.edu.uade.skillswap.application.contratacion.dto.PropuestaReprogramacionDTO;
import ar.edu.uade.skillswap.application.contratacion.dto.ReintegroDTO;
import ar.edu.uade.skillswap.domain.contratacion.Calificacion;
import ar.edu.uade.skillswap.domain.contratacion.Contratacion;
import ar.edu.uade.skillswap.domain.shared.CambioEstado;

/** Pasa la contratación del dominio a los DTOs que devuelve el Service. No tiene lógica. */
final class ContratacionMapper {

    private ContratacionMapper() {
    }

    static ContratacionDTO aDTO(Contratacion c, String tituloPublicacion) {
        var propuesta = c.propuesta() == null ? null : new PropuestaReprogramacionDTO(
                c.propuesta().propuestaPor(), c.propuesta().franjaId().valor().toString(), c.propuesta().inicio());
        var reintegro = c.reintegro() == null ? null : new ReintegroDTO(
                c.reintegro().alCliente().valor(), c.reintegro().alProveedor().valor());
        return new ContratacionDTO(c.id().valor().toString(), c.publicacionId().valor().toString(), tituloPublicacion,
                c.clienteId(), c.proveedorId(), c.franjaId().valor().toString(), c.inicioSesion(),
                c.precioAcordado().valor(), c.estado().nombre(), propuesta, reintegro,
                c.historial().stream().map(ContratacionMapper::aDTO).toList(),
                c.calificaciones().stream().map(ContratacionMapper::aDTO).toList());
    }

    static CalificacionDTO aDTO(Calificacion c) {
        return new CalificacionDTO(c.autorId(), c.destinatarioId(), c.puntaje().valor(), c.comentario(), c.fecha());
    }

    private static CambioEstadoDTO aDTO(CambioEstado c) {
        return new CambioEstadoDTO(c.desde(), c.hacia(), c.fecha(), c.actorId(), c.motivo());
    }
}
