package ar.edu.uade.skillswap.domain.contratacion.evento;

import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.shared.EventoDominio;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Un participante canceló la contratación. Le interesa a la contraparte. */
public record ContratacionCancelada(ContratacionId contratacionId, String canceladaPor, String contraparteId,
                                    String motivo, Instant ocurridoEn) implements EventoDominio {

    public ContratacionCancelada {
        Objects.requireNonNull(contratacionId, "La contratación es obligatoria");
        Objects.requireNonNull(canceladaPor, "Quien cancela es obligatorio");
        Objects.requireNonNull(contraparteId, "La contraparte es obligatoria");
        Objects.requireNonNull(motivo, "El motivo de la cancelación es obligatorio");
        Objects.requireNonNull(ocurridoEn, "La fecha del evento es obligatoria");
    }

    @Override
    public String tipo() {
        return "CONTRATACION_CANCELADA";
    }

    @Override
    public List<String> interesados() {
        return List.of(contraparteId);
    }

    @Override
    public String titulo() {
        return "Contratación cancelada";
    }

    @Override
    public String detalle() {
        return "La contratación se canceló. Motivo: " + motivo;
    }
}
