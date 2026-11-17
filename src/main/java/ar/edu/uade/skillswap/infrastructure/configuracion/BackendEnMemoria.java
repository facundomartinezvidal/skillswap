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
import ar.edu.uade.skillswap.infrastructure.memoria.BuscadorPublicacionesEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.ContratacionRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.NotificacionRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.PerfilRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.PublicacionRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.UnidadDeTrabajoEnMemoria;
import ar.edu.uade.skillswap.infrastructure.memoria.WalletRepositoryEnMemoria;
import ar.edu.uade.skillswap.infrastructure.notificacion.EmailAdapter;
import ar.edu.uade.skillswap.infrastructure.notificacion.ProveedorEmailSimulado;
import ar.edu.uade.skillswap.infrastructure.notificacion.ProveedorPushSimulado;
import ar.edu.uade.skillswap.infrastructure.notificacion.PushAdapter;

import java.time.Clock;
import java.util.Objects;
import java.util.function.Function;

/**
 * Composition root del backend en memoria: arma todos los Services con inyección manual por
 * constructor (sin Spring), con los repositorios en memoria, los adapters simulados y el publicador de
 * eventos ya suscripto. Lo usan los tests de punta a punta y sirve de plan B para la demo sin MySQL:
 * los controllers reciben estos Services. Para la versión con base de datos, se arma otro igual que
 * cambie los repositorios en memoria por los de JPA; los Services no cambian.
 */
public class BackendEnMemoria {

    /** Bono de bienvenida de la demo, en SkillCoins. */
    public static final SkillCoins BONO_DEMO = new SkillCoins(1000);

    // Infraestructura (pública para que la demo y los tests puedan ver lo que pasó)
    public final UnidadDeTrabajoEnMemoria unidadDeTrabajo = new UnidadDeTrabajoEnMemoria();
    public final PerfilRepositoryEnMemoria perfiles = new PerfilRepositoryEnMemoria(unidadDeTrabajo);
    public final PublicacionRepositoryEnMemoria publicaciones = new PublicacionRepositoryEnMemoria(unidadDeTrabajo);
    public final ContratacionRepositoryEnMemoria contrataciones = new ContratacionRepositoryEnMemoria(unidadDeTrabajo);
    public final WalletRepositoryEnMemoria wallets = new WalletRepositoryEnMemoria(unidadDeTrabajo);
    public final NotificacionRepositoryEnMemoria notificaciones = new NotificacionRepositoryEnMemoria(unidadDeTrabajo);
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

    /** Backend de demo: identidad simulada, bono de 1000, recomendación por afinidad y reloj del sistema. */
    public BackendEnMemoria() {
        this(Clock.systemUTC(), ServicioIdentidadSimulado.conUsuariosDeDemo(), BONO_DEMO, AfinidadPorHabilidades::new);
    }

    /**
     * @param estrategia cómo armar la estrategia de recomendación; recibe el repositorio de perfiles.
     *                   Cambiarla no cambia ningún Service (Strategy).
     */
    public BackendEnMemoria(Clock clock, ServicioIdentidad identidad, SkillCoins bonoBienvenida,
                            Function<PerfilRepositoryEnMemoria, EstrategiaRecomendacion> estrategia) {
        Objects.requireNonNull(clock);
        publicador.suscribir(new NotificadorDeInteresados(notificaciones, perfiles,
                new CanalEnvioFactory(new EmailAdapter(email), new PushAdapter(push))));

        perfilService = new PerfilService(perfiles, wallets, publicaciones, contrataciones, identidad,
                unidadDeTrabajo, bonoBienvenida, clock);
        publicacionService = new PublicacionService(publicaciones, perfiles, clock);
        busquedaService = new BusquedaService(new BuscadorPublicacionesEnMemoria(publicaciones, perfiles),
                publicaciones, perfiles, estrategia.apply(perfiles));
        contratacionService = new ContratacionService(contrataciones, publicaciones, wallets, perfiles,
                new RetencionDePagos(), new PoliticaReintegroFactory(ReintegroPorAnticipacion.conUmbralesPorDefecto()),
                unidadDeTrabajo, publicador, clock);
        walletService = new WalletService(wallets, clock);
        notificacionService = new NotificacionService(notificaciones, unidadDeTrabajo);
    }
}
