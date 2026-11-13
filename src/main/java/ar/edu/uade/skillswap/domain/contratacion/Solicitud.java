package ar.edu.uade.skillswap.domain.contratacion;

import ar.edu.uade.skillswap.domain.publicacion.FranjaId;

import java.util.Objects;

/** Lo que el cliente indica al solicitar una publicación. */
public record Solicitud(String mensaje, String objetivo, FranjaId franjaElegida) {

    public Solicitud {
        Objects.requireNonNull(franjaElegida, "La franja elegida es obligatoria");
    }
}
