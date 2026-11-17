package ar.edu.uade.skillswap.application.notificacion;

import ar.edu.uade.skillswap.application.evento.SuscriptorEvento;
import ar.edu.uade.skillswap.domain.notificacion.Notificacion;
import ar.edu.uade.skillswap.domain.notificacion.NotificacionRepository;
import ar.edu.uade.skillswap.domain.perfil.CanalNotificacion;
import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.perfil.PerfilRepository;
import ar.edu.uade.skillswap.domain.shared.EventoDominio;

import java.util.Objects;

/**
 * Patrón Observer: el suscriptor que avisa a las personas. Por cada interesado del evento (los declara
 * el propio evento en el dominio) deja una notificación en su bandeja y además la envía por el canal
 * que eligió en su perfil, con la {@link CanalEnvioFactory}.
 * <p>
 * Primero se guarda en la bandeja y después se envía: si el proveedor externo falla, el aviso igual
 * queda en la bandeja y los demás interesados lo reciben.
 */
public class NotificadorDeInteresados implements SuscriptorEvento {

    private static final System.Logger LOG = System.getLogger(NotificadorDeInteresados.class.getName());

    private final NotificacionRepository notificacionRepository;
    private final PerfilRepository perfilRepository;
    private final CanalEnvioFactory canales;

    public NotificadorDeInteresados(NotificacionRepository notificacionRepository, PerfilRepository perfilRepository,
                                    CanalEnvioFactory canales) {
        this.notificacionRepository = Objects.requireNonNull(notificacionRepository);
        this.perfilRepository = Objects.requireNonNull(perfilRepository);
        this.canales = Objects.requireNonNull(canales);
    }

    @Override
    public void actualizar(EventoDominio evento) {
        for (var interesado : evento.interesados()) {
            var notificacion = Notificacion.para(interesado, evento);
            notificacionRepository.save(notificacion);
            enviar(interesado, notificacion);
        }
    }

    private void enviar(String destinatarioId, Notificacion notificacion) {
        var canal = perfilRepository.findByUserId(destinatarioId)
                .map(Perfil::canal)
                .orElse(CanalNotificacion.INTERNA);
        try {
            canales.para(canal).enviar(destinatarioId, notificacion.titulo(), notificacion.detalle());
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING,
                    "No se pudo enviar por " + canal + " la notificación " + notificacion.tipo()
                            + " a " + destinatarioId + "; quedó en la bandeja", e);
        }
    }
}
