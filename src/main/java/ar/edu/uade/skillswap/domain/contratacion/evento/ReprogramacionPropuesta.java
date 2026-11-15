package ar.edu.uade.skillswap.domain.contratacion.evento;

import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.shared.EventoDominio;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Un participante propuso otra franja. Le interesa a la contraparte, que la acepta o la rechaza. */
public record ReprogramacionPropuesta(ContratacionId contratacionId, String propuestaPor, String contraparteId,
                                      Instant nuevoInicio, Instant ocurridoEn) implements EventoDominio {

    public ReprogramacionPropuesta {
        Objects.requireNonNull(contratacionId, "La contratación es obligatoria");
        Objects.requireNonNull(propuestaPor, "Quien propone es obligatorio");
        Objects.requireNonNull(contraparteId, "La contraparte es obligatoria");
        Objects.requireNonNull(nuevoInicio, "El nuevo inicio es obligatorio");
        Objects.requireNonNull(ocurridoEn, "La fecha del evento es obligatoria");
    }

    @Override
    public String tipo() {
        return "REPROGRAMACION_PROPUESTA";
    }

    @Override
    public List<String> interesados() {
        return List.of(contraparteId);
    }

    @Override
    public String titulo() {
        return "Propuesta de reprogramación";
    }

    @Override
    public String detalle() {
        return "Te propusieron cambiar la sesión al " + nuevoInicio + ". Podés aceptarla o rechazarla.";
    }
}
