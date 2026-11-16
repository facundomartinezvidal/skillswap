package ar.edu.uade.skillswap.application.evento;

import ar.edu.uade.skillswap.domain.shared.EventoDominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Patrón Observer: el sujeto. Los casos de uso le pasan los eventos que extraen de los agregados
 * después de guardar, y él se los entrega a cada suscriptor en el orden en que se suscribieron.
 * Así, agregar una reacción nueva a un evento es agregar un suscriptor, sin tocar el caso de uso.
 * <p>
 * Como la operación ya quedó guardada, un suscriptor que falla (por ejemplo, el envío de un email)
 * no la deshace ni corta a los demás: el error se registra y se sigue con el siguiente.
 */
public final class PublicadorEventos {

    private static final System.Logger LOG = System.getLogger(PublicadorEventos.class.getName());

    private final List<SuscriptorEvento> suscriptores = new ArrayList<>();

    /** Suscribir dos veces la misma instancia no la hace recibir los eventos dos veces. */
    public void suscribir(SuscriptorEvento suscriptor) {
        Objects.requireNonNull(suscriptor, "El suscriptor es obligatorio");
        // Por identidad y no con equals: dos suscriptores distintos pueden ser iguales (por ejemplo, records).
        if (suscriptores.stream().noneMatch(existente -> existente == suscriptor)) {
            suscriptores.add(suscriptor);
        }
    }

    public void publicar(List<EventoDominio> eventos) {
        Objects.requireNonNull(eventos, "Los eventos son obligatorios");
        for (var evento : eventos) {
            for (var suscriptor : suscriptores) {
                avisar(suscriptor, evento);
            }
        }
    }

    private static void avisar(SuscriptorEvento suscriptor, EventoDominio evento) {
        try {
            suscriptor.actualizar(evento);
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING,
                    "El suscriptor " + suscriptor.getClass().getSimpleName() + " falló con el evento " + evento.tipo(), e);
        }
    }
}
