package ar.edu.uade.skillswap.domain.contratacion.evento;

import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.shared.EventoDominio;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** El proveedor aceptó la solicitud. Le interesa al cliente, que ya puede pagar. */
public record ContratacionAceptada(ContratacionId contratacionId, String clienteId, String proveedorId,
                                   Instant ocurridoEn) implements EventoDominio {

    public ContratacionAceptada {
        Objects.requireNonNull(contratacionId, "La contratación es obligatoria");
        Objects.requireNonNull(clienteId, "El cliente es obligatorio");
        Objects.requireNonNull(proveedorId, "El proveedor es obligatorio");
        Objects.requireNonNull(ocurridoEn, "La fecha del evento es obligatoria");
    }

    @Override
    public String tipo() {
        return "CONTRATACION_ACEPTADA";
    }

    @Override
    public List<String> interesados() {
        return List.of(clienteId);
    }

    @Override
    public String titulo() {
        return "Solicitud aceptada";
    }

    @Override
    public String detalle() {
        return "El proveedor aceptó tu solicitud. Pagá para confirmar la sesión.";
    }
}
