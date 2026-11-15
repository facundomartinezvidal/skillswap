package ar.edu.uade.skillswap.domain.contratacion.evento;

import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.shared.EventoDominio;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** El cliente pagó y la contratación quedó confirmada. Le interesa al proveedor. */
public record PagoConfirmado(ContratacionId contratacionId, String clienteId, String proveedorId,
                             Instant ocurridoEn) implements EventoDominio {

    public PagoConfirmado {
        Objects.requireNonNull(contratacionId, "La contratación es obligatoria");
        Objects.requireNonNull(clienteId, "El cliente es obligatorio");
        Objects.requireNonNull(proveedorId, "El proveedor es obligatorio");
        Objects.requireNonNull(ocurridoEn, "La fecha del evento es obligatoria");
    }

    @Override
    public String tipo() {
        return "PAGO_CONFIRMADO";
    }

    @Override
    public List<String> interesados() {
        return List.of(proveedorId);
    }

    @Override
    public String titulo() {
        return "Pago confirmado";
    }

    @Override
    public String detalle() {
        return "El cliente pagó la contratación. La sesión quedó confirmada.";
    }
}
