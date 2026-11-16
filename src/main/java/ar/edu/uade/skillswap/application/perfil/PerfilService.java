package ar.edu.uade.skillswap.application.perfil;

import ar.edu.uade.skillswap.application.perfil.dto.CrearPerfilCommand;
import ar.edu.uade.skillswap.application.perfil.dto.DisponibilidadDTO;
import ar.edu.uade.skillswap.application.perfil.dto.EditarPerfilCommand;
import ar.edu.uade.skillswap.application.perfil.dto.PerfilDTO;
import ar.edu.uade.skillswap.application.shared.UnidadDeTrabajo;
import ar.edu.uade.skillswap.domain.contratacion.ContratacionRepository;
import ar.edu.uade.skillswap.domain.perfil.CanalNotificacion;
import ar.edu.uade.skillswap.domain.perfil.PerfilRepository;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionRepository;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.wallet.WalletRepository;

import java.time.Clock;
import java.util.Objects;

/** Casos de uso del perfil. Lo usa el {@code PerfilController}. */
public class PerfilService {

    private final PerfilRepository perfilRepository;
    private final WalletRepository walletRepository;
    private final PublicacionRepository publicacionRepository;
    private final ContratacionRepository contratacionRepository;
    private final ServicioIdentidad servicioIdentidad;
    private final UnidadDeTrabajo unidadDeTrabajo;
    private final SkillCoins bonoBienvenida;
    private final Clock clock;

    public PerfilService(PerfilRepository perfilRepository, WalletRepository walletRepository,
                         PublicacionRepository publicacionRepository, ContratacionRepository contratacionRepository,
                         ServicioIdentidad servicioIdentidad, UnidadDeTrabajo unidadDeTrabajo,
                         SkillCoins bonoBienvenida, Clock clock) {
        this.perfilRepository = Objects.requireNonNull(perfilRepository);
        this.walletRepository = Objects.requireNonNull(walletRepository);
        this.publicacionRepository = Objects.requireNonNull(publicacionRepository);
        this.contratacionRepository = Objects.requireNonNull(contratacionRepository);
        this.servicioIdentidad = Objects.requireNonNull(servicioIdentidad);
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
        this.bonoBienvenida = Objects.requireNonNull(bonoBienvenida);
        this.clock = Objects.requireNonNull(clock);
    }

    /** Crea el perfil y le abre la Wallet con el bono de bienvenida, en una transacción. */
    public PerfilDTO crearPerfil(CrearPerfilCommand command) {
        throw pendiente();
    }

    public PerfilDTO editarPerfil(String userId, EditarPerfilCommand command) {
        throw pendiente();
    }

    public PerfilDTO agregarDisponibilidad(String userId, DisponibilidadDTO disponibilidad) {
        throw pendiente();
    }

    public PerfilDTO quitarDisponibilidad(String userId, DisponibilidadDTO disponibilidad) {
        throw pendiente();
    }

    public PerfilDTO elegirCanal(String userId, CanalNotificacion canal) {
        throw pendiente();
    }

    /** Baja lógica (regla 9). */
    public void darDeBaja(String userId, String motivo) {
        throw pendiente();
    }

    /** Perfil público de cualquier usuario. */
    public PerfilDTO verPerfil(String userId) {
        throw pendiente();
    }

    private static UnsupportedOperationException pendiente() {
        return new UnsupportedOperationException("Pendiente: se implementa en un PR siguiente");
    }
}
