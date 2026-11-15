package ar.edu.uade.skillswap.domain.contratacion.evento;

import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.shared.EventoDominio;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** La contraparte aceptó la reprogramación. Le interesa a quien la propuso. */
public record ReprogramacionAceptada(ContratacionId contratacionId, String propuestaPor, String aceptadaPor,
                                     Instant nuevoInicio, Instant ocurridoEn) implements EventoDominio {

    public ReprogramacionAceptada {
        Objects.requireNonNull(contratacionId, "La contratación es obligatoria");
        Objects.requireNonNull(propuestaPor, "Quien propuso es obligatorio");
        Objects.requireNonNull(aceptadaPor, "Quien acepta es obligatorio");
        Objects.requireNonNull(nuevoInicio, "El nuevo inicio es obligatorio");
        Objects.requireNonNull(ocurridoEn, "La fecha del evento es obligatoria");
    }

    @Override
    public String tipo() {
        return "REPROGRAMACION_ACEPTADA";
    }

    @Override
    public List<String> interesados() {
        return List.of(propuestaPor);
    }

    @Override
    public String titulo() {
        return "Reprogramación aceptada";
    }

    @Override
    public String detalle() {
        return "Aceptaron tu propuesta: la sesión pasa al " + nuevoInicio + ".";
    }
}
