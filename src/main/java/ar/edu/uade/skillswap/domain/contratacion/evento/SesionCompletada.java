package ar.edu.uade.skillswap.domain.contratacion.evento;

import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.shared.EventoDominio;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** El proveedor completó la sesión. Les interesa a los dos participantes, que ya pueden calificar. */
public record SesionCompletada(ContratacionId contratacionId, String clienteId, String proveedorId,
                               Instant ocurridoEn) implements EventoDominio {

    public SesionCompletada {
        Objects.requireNonNull(contratacionId, "La contratación es obligatoria");
        Objects.requireNonNull(clienteId, "El cliente es obligatorio");
        Objects.requireNonNull(proveedorId, "El proveedor es obligatorio");
        Objects.requireNonNull(ocurridoEn, "La fecha del evento es obligatoria");
    }

    @Override
    public String tipo() {
        return "SESION_COMPLETADA";
    }

    @Override
    public List<String> interesados() {
        return List.of(clienteId, proveedorId);
    }

    @Override
    public String titulo() {
        return "Sesión completada";
    }

    @Override
    public String detalle() {
        return "La sesión terminó. Ya podés calificar a la otra parte.";
    }
}
