package ar.edu.uade.skillswap.controllers.impl;

import ar.edu.uade.skillswap.application.busqueda.BusquedaService;
import ar.edu.uade.skillswap.application.contratacion.ContratacionService;
import ar.edu.uade.skillswap.application.notificacion.NotificacionService;
import ar.edu.uade.skillswap.application.perfil.PerfilService;
import ar.edu.uade.skillswap.application.publicacion.PublicacionService;
import ar.edu.uade.skillswap.application.wallet.WalletService;
import ar.edu.uade.skillswap.controllers.CalificacionController;
import ar.edu.uade.skillswap.controllers.RecomendacionController;
import ar.edu.uade.skillswap.controllers.UiControllers;

import java.time.Clock;

/**
 * Los controllers reales que recibe la UI de la cátedra, armados con inyección manual por constructor.
 * No sabe si los Services trabajan en memoria o con JPA. Los getters devuelven las clases concretas
 * (tipos de retorno covariantes) para que también se vean las operaciones del equipo, como
 * {@code contrataciones().pagar(...)}, sin cambiar {@link UiControllers}.
 */
public class SkillSwapControllers implements UiControllers {

    private final RecomendacionControllerImpl recomendaciones;
    private final BusquedaControllerImpl busqueda;
    private final PublicacionControllerImpl publicaciones;
    private final ContratacionControllerImpl contrataciones;
    private final CalificacionControllerImpl calificaciones;
    private final WalletControllerImpl wallet;
    private final NotificacionControllerImpl notificaciones;
    private final PerfilControllerImpl perfiles;

    public SkillSwapControllers(PerfilService perfilService, PublicacionService publicacionService,
                                BusquedaService busquedaService, ContratacionService contratacionService,
                                WalletService walletService, NotificacionService notificacionService, Clock clock) {
        recomendaciones = new RecomendacionControllerImpl(busquedaService);
        busqueda = new BusquedaControllerImpl(busquedaService, recomendaciones);
        publicaciones = new PublicacionControllerImpl(publicacionService, clock);
        contrataciones = new ContratacionControllerImpl(contratacionService, publicacionService, perfilService, clock);
        calificaciones = new CalificacionControllerImpl(contratacionService, perfilService);
        wallet = new WalletControllerImpl(walletService);
        notificaciones = new NotificacionControllerImpl(notificacionService);
        perfiles = new PerfilControllerImpl(perfilService);
    }

    @Override
    public BusquedaControllerImpl busqueda() {
        return busqueda;
    }

    @Override
    public PublicacionControllerImpl publicaciones() {
        return publicaciones;
    }

    @Override
    public ContratacionControllerImpl contrataciones() {
        return contrataciones;
    }

    @Override
    public WalletControllerImpl wallet() {
        return wallet;
    }

    @Override
    public NotificacionControllerImpl notificaciones() {
        return notificaciones;
    }

    @Override
    public PerfilControllerImpl perfiles() {
        return perfiles;
    }

    public RecomendacionController recomendaciones() {
        return recomendaciones;
    }

    public CalificacionController calificaciones() {
        return calificaciones;
    }
}
