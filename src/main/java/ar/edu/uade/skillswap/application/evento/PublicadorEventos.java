package ar.edu.uade.skillswap.application.evento;

import ar.edu.uade.skillswap.domain.shared.EventoDominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Patrón Observer: el sujeto. Los casos de uso le pasan los eventos que extraen de los agregados
 * después de guardar, y él se los entrega a cada suscriptor en el orden en que se suscribieron.
 * Así, agregar una reacción nueva a un evento es agregar un suscriptor, sin tocar el caso de uso.
 */
public final class PublicadorEventos {

    private final List<SuscriptorEvento> suscriptores = new ArrayList<>();

    /** Suscribir dos veces al mismo suscriptor no lo hace recibir los eventos dos veces. */
    public void suscribir(SuscriptorEvento suscriptor) {
        Objects.requireNonNull(suscriptor, "El suscriptor es obligatorio");
        if (!suscriptores.contains(suscriptor)) {
            suscriptores.add(suscriptor);
        }
    }

    public void publicar(List<EventoDominio> eventos) {
        Objects.requireNonNull(eventos, "Los eventos son obligatorios");
        for (var evento : eventos) {
            for (var suscriptor : suscriptores) {
                suscriptor.actualizar(evento);
            }
        }
    }
}
