package ar.edu.uade.skillswap.infrastructure.configuracion;

import ar.edu.uade.skillswap.application.busqueda.BusquedaService;
import ar.edu.uade.skillswap.application.busqueda.EstrategiaRecomendacion;
import ar.edu.uade.skillswap.application.busqueda.recomendacion.AfinidadPorHabilidades;
import ar.edu.uade.skillswap.application.contratacion.ContratacionService;
import ar.edu.uade.skillswap.application.evento.PublicadorEventos;
import ar.edu.uade.skillswap.application.notificacion.CanalEnvioFactory;
import ar.edu.uade.skillswap.application.notificacion.NotificacionService;
import ar.edu.uade.skillswap.application.notificacion.NotificadorDeInteresados;
import ar.edu.uade.skillswap.application.perfil.PerfilService;
import ar.edu.uade.skillswap.application.perfil.ServicioIdentidad;
import ar.edu.uade.skillswap.application.publicacion.PublicacionService;
import ar.edu.uade.skillswap.application.wallet.WalletService;
import ar.edu.uade.skillswap.domain.contratacion.strategy.PoliticaReintegroFactory;
import ar.edu.uade.skillswap.domain.contratacion.strategy.ReintegroPorAnticipacion;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.wallet.RetencionDePagos;
import ar.edu.uade.skillswap.infrastructure.identidad.ServicioIdentidadSimulado;
import ar.edu.uade.skillswap.infrastructure.notificacion.EmailAdapter;
import ar.edu.uade.skillswap.infrastructure.notificacion.ProveedorEmailSimulado;
import ar.edu.uade.skillswap.infrastructure.notificacion.ProveedorPushSimulado;
import ar.edu.uade.skillswap.infrastructure.notificacion.PushAdapter;
import ar.edu.uade.skillswap.infrastructure.persistencia.ConfiguracionBaseDeDatos;
import ar.edu.uade.skillswap.infrastructure.persistencia.Migraciones;
import ar.edu.uade.skillswap.infrastructure.persistencia.Persistencia;
import ar.edu.uade.skillswap.infrastructure.persistencia.UnidadDeTrabajoJpa;
import ar.edu.uade.skillswap.infrastructure.persistencia.contratacion.ContratacionRepositoryJpa;
import ar.edu.uade.skillswap.infrastructure.persistencia.notificacion.NotificacionRepositoryJpa;
import ar.edu.uade.skillswap.infrastructure.persistencia.perfil.PerfilRepositoryJpa;
import ar.edu.uade.skillswap.infrastructure.persistencia.publicacion.BuscadorPublicacionesJpa;
import ar.edu.uade.skillswap.infrastructure.persistencia.publicacion.PublicacionRepositoryJpa;
import ar.edu.uade.skillswap.infrastructure.persistencia.wallet.WalletRepositoryJpa;

import jakarta.persistence.EntityManagerFactory;

import java.time.Clock;
import java.util.Objects;
import java.util.function.Function;

/**
 * Composition root del backend con MySQL: igual que {@link BackendEnMemoria} (mismos Services, mismos
 * adapters simulados y el mismo publicador de eventos suscripto), pero con los repositorios JPA y la
 * {@link UnidadDeTrabajoJpa}. Al construirse corre las migraciones de Flyway. Los Services no cambian:
 * solo cambia qué implementación de cada puerto reciben.
 */
public class BackendJpa implements AutoCloseable {

    // Infraestructura (pública para que la demo y los tests puedan ver lo que pasó)
    public final EntityManagerFactory entityManagerFactory;
    public final UnidadDeTrabajoJpa unidadDeTrabajo;
    public final PerfilRepositoryJpa perfiles;
    public final PublicacionRepositoryJpa publicaciones;
    public final ContratacionRepositoryJpa contrataciones;
    public final WalletRepositoryJpa wallets;
    public final NotificacionRepositoryJpa notificaciones;
    public final ProveedorEmailSimulado email = new ProveedorEmailSimulado();
    public final ProveedorPushSimulado push = new ProveedorPushSimulado();
    public final PublicadorEventos publicador = new PublicadorEventos();

    // Casos de uso
    public final PerfilService perfilService;
    public final PublicacionService publicacionService;
    public final BusquedaService busquedaService;
    public final ContratacionService contratacionService;
    public final WalletService walletService;
    public final NotificacionService notificacionService;

    /** Backend de demo contra la base de {@code DB_URL}: identidad simulada, bono de 1000 y afinidad. */
    public BackendJpa() {
        this(ConfiguracionBaseDeDatos.desdeEntorno(), Clock.systemUTC(), ServicioIdentidadSimulado.conUsuariosDeDemo(),
                BackendEnMemoria.BONO_DEMO, AfinidadPorHabilidades::new);
    }

    /**
     * @param estrategia cómo armar la estrategia de recomendación; recibe el repositorio de perfiles.
     *                   Cambiarla no cambia ningún Service (Strategy).
     */
    public BackendJpa(ConfiguracionBaseDeDatos baseDeDatos, Clock clock, ServicioIdentidad identidad,
                      SkillCoins bonoBienvenida, Function<PerfilRepositoryJpa, EstrategiaRecomendacion> estrategia) {
        Objects.requireNonNull(clock);
        Migraciones.aplicar(baseDeDatos);
        entityManagerFactory = Persistencia.crearEntityManagerFactory(baseDeDatos);
        unidadDeTrabajo = new UnidadDeTrabajoJpa(entityManagerFactory);
        perfiles = new PerfilRepositoryJpa(unidadDeTrabajo);
        publicaciones = new PublicacionRepositoryJpa(unidadDeTrabajo);
        contrataciones = new ContratacionRepositoryJpa(unidadDeTrabajo);
        wallets = new WalletRepositoryJpa(unidadDeTrabajo);
        notificaciones = new NotificacionRepositoryJpa(unidadDeTrabajo);

        publicador.suscribir(new NotificadorDeInteresados(notificaciones, perfiles,
                new CanalEnvioFactory(new EmailAdapter(email), new PushAdapter(push))));

        perfilService = new PerfilService(perfiles, wallets, publicaciones, contrataciones, identidad,
                unidadDeTrabajo, bonoBienvenida, clock);
        publicacionService = new PublicacionService(publicaciones, perfiles, clock);
        busquedaService = new BusquedaService(new BuscadorPublicacionesJpa(unidadDeTrabajo), publicaciones, perfiles,
                estrategia.apply(perfiles));
        contratacionService = new ContratacionService(contrataciones, publicaciones, wallets, perfiles,
                new RetencionDePagos(), new PoliticaReintegroFactory(ReintegroPorAnticipacion.conUmbralesPorDefecto()),
                unidadDeTrabajo, publicador, clock);
        walletService = new WalletService(wallets, clock);
        notificacionService = new NotificacionService(notificaciones, unidadDeTrabajo);
    }

    /** Cierra las conexiones a la base. */
    @Override
    public void close() {
        entityManagerFactory.close();
    }
}
