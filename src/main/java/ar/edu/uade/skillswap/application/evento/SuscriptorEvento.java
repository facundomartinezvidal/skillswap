package ar.edu.uade.skillswap.application.evento;

import ar.edu.uade.skillswap.domain.shared.EventoDominio;

/** Patrón Observer: el observador. Reacciona a los eventos que le pasa el {@link PublicadorEventos}. */
public interface SuscriptorEvento {

    void actualizar(EventoDominio evento);
}
