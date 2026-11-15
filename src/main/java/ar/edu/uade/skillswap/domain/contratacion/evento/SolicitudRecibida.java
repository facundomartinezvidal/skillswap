package ar.edu.uade.skillswap.domain.contratacion.evento;

import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.shared.EventoDominio;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Un cliente solicitó una publicación. Le interesa al proveedor. */
public record SolicitudRecibida(ContratacionId contratacionId, String clienteId, String proveedorId,
                                Instant ocurridoEn) implements EventoDominio {

    public SolicitudRecibida {
        Objects.requireNonNull(contratacionId, "La contratación es obligatoria");
        Objects.requireNonNull(clienteId, "El cliente es obligatorio");
        Objects.requireNonNull(proveedorId, "El proveedor es obligatorio");
        Objects.requireNonNull(ocurridoEn, "La fecha del evento es obligatoria");
    }

    @Override
    public String tipo() {
        return "SOLICITUD_RECIBIDA";
    }

    @Override
    public List<String> interesados() {
        return List.of(proveedorId);
    }

    @Override
    public String titulo() {
        return "Nueva solicitud";
    }

    @Override
    public String detalle() {
        return "Recibiste una solicitud de contratación. Podés aceptarla o rechazarla.";
    }
}
