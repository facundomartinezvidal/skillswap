package ar.edu.uade.skillswap.application.perfil;

import ar.edu.uade.skillswap.application.perfil.dto.CrearPerfilCommand;
import ar.edu.uade.skillswap.application.perfil.dto.DisponibilidadDTO;
import ar.edu.uade.skillswap.application.perfil.dto.EditarPerfilCommand;
import ar.edu.uade.skillswap.application.perfil.dto.PerfilDTO;
import ar.edu.uade.skillswap.application.perfil.exception.PerfilDuplicadoException;
import ar.edu.uade.skillswap.application.publicacion.PublicacionMapper;
import ar.edu.uade.skillswap.application.shared.UnidadDeTrabajo;
import ar.edu.uade.skillswap.application.shared.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.domain.contratacion.ContratacionRepository;
import ar.edu.uade.skillswap.domain.perfil.CanalNotificacion;
import ar.edu.uade.skillswap.domain.perfil.DisponibilidadSemanal;
import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.perfil.PerfilRepository;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionRepository;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;
import ar.edu.uade.skillswap.domain.wallet.Wallet;
import ar.edu.uade.skillswap.domain.wallet.WalletRepository;

import java.time.Clock;
import java.util.Objects;
import java.util.Set;

/**
 * Casos de uso del perfil. Lo usa el {@code PerfilController}.
 * El userId que recibe cada operación es el del usuario activo, así que cada uno solo edita su perfil
 * (regla 10). Las reglas (baja lógica, disponibilidad sin superposiciones) las valida el {@link Perfil}.
 */
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

    /**
     * Crea el perfil de un usuario que existe en el servicio de identidad y le abre la Wallet con el
     * bono de bienvenida, en una transacción. Un usuario tiene un solo perfil.
     */
    public PerfilDTO crearPerfil(CrearPerfilCommand command) {
        if (!servicioIdentidad.existe(command.userId())) {
            throw new RecursoNoEncontradoException("usuario", command.userId());
        }
        if (perfilRepository.findByUserId(command.userId()).isPresent()) {
            throw new PerfilDuplicadoException(command.userId());
        }
        var perfil = unidadDeTrabajo.ejecutar(() -> {
            var nuevo = Perfil.crear(command.userId(), command.tipo(), command.nombreVisible(), command.zonaHoraria());
            perfilRepository.save(nuevo);
            if (walletRepository.findByTitular(command.userId()).isEmpty()) {
                walletRepository.save(Wallet.abrir(command.userId(), bonoBienvenida, clock.instant()));
            }
            return nuevo;
        });
        return aDTO(perfil);
    }

    /** Reemplaza nombre, biografía, zona horaria, habilidades e intereses. Un conjunto nulo queda vacío. */
    public PerfilDTO editarPerfil(String userId, EditarPerfilCommand command) {
        var perfil = buscar(userId);
        perfil.editarDatos(command.nombreVisible(), command.biografia(), command.zonaHoraria());
        perfil.definirHabilidades(vacioSiEsNulo(command.habilidades()));
        perfil.definirIntereses(vacioSiEsNulo(command.intereses()));
        return guardar(perfil);
    }

    public PerfilDTO agregarDisponibilidad(String userId, DisponibilidadDTO disponibilidad) {
        var perfil = buscar(userId);
        perfil.agregarDisponibilidad(disponibilidad.dia(), disponibilidad.inicio(), disponibilidad.fin());
        return guardar(perfil);
    }

    public PerfilDTO quitarDisponibilidad(String userId, DisponibilidadDTO disponibilidad) {
        var perfil = buscar(userId);
        perfil.quitarDisponibilidad(
                new DisponibilidadSemanal(disponibilidad.dia(), disponibilidad.inicio(), disponibilidad.fin()));
        return guardar(perfil);
    }

    public PerfilDTO elegirCanal(String userId, CanalNotificacion canal) {
        var perfil = buscar(userId);
        perfil.elegirCanal(canal);
        return guardar(perfil);
    }

    /** Baja lógica (regla 9): el perfil y sus operaciones quedan, pero ya no se puede editar. */
    public void darDeBaja(String userId, String motivo) {
        var perfil = buscar(userId);
        perfil.darDeBaja(userId, motivo, clock.instant());
        perfilRepository.save(perfil);
    }

    /** Perfil público de cualquier usuario: reputación, publicaciones activas e historial resumido. */
    public PerfilDTO verPerfil(String userId) {
        return aDTO(buscar(userId));
    }

    private Perfil buscar(String userId) {
        return perfilRepository.findByUserId(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException("perfil", userId));
    }

    private PerfilDTO guardar(Perfil perfil) {
        perfilRepository.save(perfil);
        return aDTO(perfil);
    }

    private PerfilDTO aDTO(Perfil perfil) {
        var activas = publicacionRepository.findActivasByAutor(perfil.userId()).stream()
                .map(publicacion -> PublicacionMapper.aDTO(publicacion, perfil))
                .toList();
        var completadas = contratacionRepository.countCompletadasDe(perfil.userId());
        return PerfilMapper.aDTO(perfil, completadas, activas);
    }

    private static Set<String> vacioSiEsNulo(Set<String> valores) {
        return valores == null ? Set.of() : valores;
    }
}
