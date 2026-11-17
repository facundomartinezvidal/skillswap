package ar.edu.uade.skillswap.application.publicacion;

import ar.edu.uade.skillswap.application.publicacion.dto.DatosPublicacionCommand;
import ar.edu.uade.skillswap.application.publicacion.dto.PromocionDTO;
import ar.edu.uade.skillswap.application.publicacion.dto.PublicacionDTO;
import ar.edu.uade.skillswap.application.shared.Ids;
import ar.edu.uade.skillswap.application.shared.UnidadDeTrabajo;
import ar.edu.uade.skillswap.application.shared.exception.PermisoDenegadoException;
import ar.edu.uade.skillswap.application.shared.exception.RecursoNoEncontradoException;
import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.perfil.PerfilRepository;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.domain.perfil.exception.PerfilInactivoException;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionId;
import ar.edu.uade.skillswap.domain.publicacion.PublicacionRepository;
import ar.edu.uade.skillswap.domain.publicacion.TipoPublicacion;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Casos de uso de las publicaciones. Lo usa el {@code PublicacionController}.
 * Todas las operaciones que modifican validan que quien actúa sea el autor (regla 10); los estados,
 * los requisitos para publicar y las promociones los valida la {@link Publicacion}.
 * Toda operación que modifica corre en una {@link UnidadDeTrabajo}, aunque toque un solo agregado: la
 * lectura y el guardado tienen que estar en la misma transacción para que el bloqueo optimista detecte
 * los cambios concurrentes (por ejemplo, una edición que se cruza con una aceptación que reservó cupo).
 */
public class PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final PerfilRepository perfilRepository;
    private final UnidadDeTrabajo unidadDeTrabajo;
    private final Clock clock;

    public PublicacionService(PublicacionRepository publicacionRepository, PerfilRepository perfilRepository,
                              UnidadDeTrabajo unidadDeTrabajo, Clock clock) {
        this.publicacionRepository = Objects.requireNonNull(publicacionRepository);
        this.perfilRepository = Objects.requireNonNull(perfilRepository);
        this.unidadDeTrabajo = Objects.requireNonNull(unidadDeTrabajo);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Crea el borrador. El tipo sale del perfil del autor: un profesional publica ofertas y una
     * organización, desafíos. Como el tipo de perfil no cambia, la publicación siempre coincide con su
     * autor y {@link #publicar} no necesita volver a validarlo.
     */
    public PublicacionDTO crearBorrador(String userId, DatosPublicacionCommand datos) {
        return unidadDeTrabajo.ejecutar(() -> {
            var autor = perfilActivo(userId, "crear una publicación");
            var tipo = autor.tipo() == TipoPerfil.PROFESIONAL ? TipoPublicacion.OFERTA : TipoPublicacion.DESAFIO;
            var publicacion = Publicacion.crearBorrador(userId, tipo);
            editarCon(publicacion, datos);
            publicacionRepository.save(publicacion);
            return PublicacionMapper.aDTO(publicacion, autor);
        });
    }

    public PublicacionDTO editar(String userId, String publicacionId, DatosPublicacionCommand datos) {
        return modificar(userId, publicacionId, "editar", publicacion -> editarCon(publicacion, datos));
    }

    public PublicacionDTO agregarFranja(String userId, String publicacionId, Instant inicio, Instant fin) {
        return modificar(userId, publicacionId, "agregar franjas a",
                publicacion -> publicacion.agregarFranja(inicio, fin));
    }

    /** Un perfil dado de baja no publica. Los requisitos para publicar los valida la {@link Publicacion}. */
    public PublicacionDTO publicar(String userId, String publicacionId) {
        return modificar(userId, publicacionId, "publicar", publicacion -> {
            perfilActivo(userId, "publicar");
            publicacion.publicar(userId, ahora());
        });
    }

    public PublicacionDTO pausar(String userId, String publicacionId, String motivo) {
        return modificar(userId, publicacionId, "pausar", publicacion -> publicacion.pausar(userId, motivo, ahora()));
    }

    /** Igual que al publicar: un perfil dado de baja no vuelve a poner la publicación en PUBLICADA. */
    public PublicacionDTO reanudar(String userId, String publicacionId) {
        return modificar(userId, publicacionId, "reanudar", publicacion -> {
            perfilActivo(userId, "reanudar");
            publicacion.reanudar(userId, ahora());
        });
    }

    public PublicacionDTO cerrar(String userId, String publicacionId, String motivo) {
        return modificar(userId, publicacionId, "cerrar", publicacion -> publicacion.cerrar(userId, motivo, ahora()));
    }

    public PublicacionDTO cancelar(String userId, String publicacionId, String motivo) {
        return modificar(userId, publicacionId, "cancelar", publicacion -> publicacion.cancelar(userId, motivo, ahora()));
    }

    public PublicacionDTO agregarPromocion(String userId, String publicacionId, PromocionDTO promocion) {
        return modificar(userId, publicacionId, "agregar promociones a",
                publicacion -> publicacion.agregarPromocion(PublicacionMapper.aPromocion(promocion)));
    }

    public PublicacionDTO quitarPromocion(String userId, String publicacionId, PromocionDTO promocion) {
        return modificar(userId, publicacionId, "quitar promociones de",
                publicacion -> publicacion.quitarPromocion(PublicacionMapper.aPromocion(promocion)));
    }

    /** Panel del autor: sus publicaciones en cualquier estado. */
    public Pagina<PublicacionDTO> listarPropias(String userId, PaginaSolicitada pagina) {
        var autor = perfilRepository.findByUserId(userId).orElse(null);
        return publicacionRepository.findByAutor(userId, pagina).map(p -> PublicacionMapper.aDTO(p, autor));
    }

    /**
     * Detalle con franjas y lugares libres, para elegir al solicitar. Un BORRADOR o una PAUSADA solo
     * la ve su autor; para los demás es como si no existiera.
     */
    public PublicacionDTO verPublicacion(String userId, String publicacionId) {
        var publicacion = buscar(publicacionId);
        if (!publicacion.esVisiblePara(userId)) {
            throw new RecursoNoEncontradoException("publicación", publicacionId);
        }
        return aDTO(publicacion);
    }

    private void editarCon(Publicacion publicacion, DatosPublicacionCommand datos) {
        var precio = datos.precioBase() == null ? null : new SkillCoins(datos.precioBase());
        publicacion.editar(datos.titulo(), datos.descripcion(), datos.categoria(), datos.modalidad(),
                datos.duracionEstimada(), datos.cupo(), precio, datos.etiquetas());
    }

    private Publicacion buscar(String publicacionId) {
        var id = new PublicacionId(Ids.uuid("publicación", publicacionId));
        return publicacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("publicación", publicacionId));
    }

    /** Regla 10: solo el autor modifica su publicación. */
    private Publicacion buscarPropia(String userId, String publicacionId, String accion) {
        var publicacion = buscar(publicacionId);
        if (!publicacion.autorId().equals(userId)) {
            throw new PermisoDenegadoException(userId, accion + " una publicación ajena");
        }
        return publicacion;
    }

    private Perfil perfilActivo(String userId, String accion) {
        var perfil = perfilRepository.findByUserId(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException("perfil", userId));
        if (!perfil.estaActivo()) {
            throw new PerfilInactivoException(userId, accion);
        }
        return perfil;
    }

    /**
     * Lee la publicación del autor, le aplica el cambio y la guarda, todo en una transacción. Así el
     * bloqueo optimista compara contra la versión que se leyó: si en el medio otra operación (por ejemplo,
     * una aceptación que reservó cupo) la cambió, esta falla con un conflicto en lugar de pisar el cambio.
     */
    private PublicacionDTO modificar(String userId, String publicacionId, String accion, Consumer<Publicacion> cambio) {
        return unidadDeTrabajo.ejecutar(() -> {
            var publicacion = buscarPropia(userId, publicacionId, accion);
            cambio.accept(publicacion);
            publicacionRepository.save(publicacion);
            return aDTO(publicacion);
        });
    }

    private PublicacionDTO aDTO(Publicacion publicacion) {
        return PublicacionMapper.aDTO(publicacion, perfilRepository.findByUserId(publicacion.autorId()).orElse(null));
    }

    private Instant ahora() {
        return clock.instant();
    }
}
