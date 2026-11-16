package ar.edu.uade.skillswap.controllers.impl;

import ar.edu.uade.skillswap.application.publicacion.PublicacionService;
import ar.edu.uade.skillswap.application.publicacion.dto.DatosPublicacionCommand;
import ar.edu.uade.skillswap.application.publicacion.dto.PromocionDTO;
import ar.edu.uade.skillswap.application.publicacion.dto.PublicacionDTO;
import ar.edu.uade.skillswap.controllers.OperacionRechazadaException;
import ar.edu.uade.skillswap.controllers.PublicacionOperaciones;
import ar.edu.uade.skillswap.controllers.UiControllers;
import ar.edu.uade.skillswap.model.Models.EstadoPublicacion;
import ar.edu.uade.skillswap.model.Models.Modalidad;
import ar.edu.uade.skillswap.model.Models.Publicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static ar.edu.uade.skillswap.controllers.impl.ManejoDeErrores.ejecutar;

public class PublicacionControllerImpl implements UiControllers.PublicacionController, PublicacionOperaciones {

    /** El formulario de la UI no pide cupo: se publica para una persona por franja. */
    static final int CUPO_POR_DEFECTO = 1;

    /** El formulario de la UI no pide motivo al pausar, cerrar o cancelar. */
    static final String MOTIVO_POR_DEFECTO = "Cambio desde la aplicación";

    private final PublicacionService publicacionService;
    private final Clock clock;

    public PublicacionControllerImpl(PublicacionService publicacionService, Clock clock) {
        this.publicacionService = Objects.requireNonNull(publicacionService);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public List<Publicacion> listarPropias(String userId) {
        return ejecutar(() -> publicacionService.listarPropias(userId, ModelosMapper.PRIMERA_PAGINA)
                .elementos().stream()
                .map(ModelosMapper::aModelo)
                .toList());
    }

    /**
     * El formulario de la UI no pide cupo ni franjas. Para que el borrador se pueda publicar desde la UI,
     * se crea con cupo 1 y una franja dentro de una semana, a la misma hora en punto y con la duración
     * indicada (en minutos, como la muestra la UI).
     */
    @Override
    public Publicacion crearBorrador(String userId, String titulo, String descripcion, String categoria,
                                     Modalidad modalidad, int duracion, int precio) {
        return ejecutar(() -> {
            var datos = new DatosPublicacionCommand(titulo, descripcion, categoria, ModelosMapper.aDominio(modalidad),
                    Duration.ofMinutes(duracion), CUPO_POR_DEFECTO, (long) precio,
                    categoria == null ? Set.of() : Set.of(categoria));
            var borrador = publicacionService.crearBorrador(userId, datos);
            if (duracion <= 0) {
                return ModelosMapper.aModelo(borrador);
            }
            var inicio = clock.instant().truncatedTo(ChronoUnit.HOURS).plus(Duration.ofDays(7));
            var conFranja = publicacionService.agregarFranja(userId, borrador.id(), inicio,
                    inicio.plus(Duration.ofMinutes(duracion)));
            return ModelosMapper.aModelo(conFranja);
        });
    }

    /**
     * La UI pide el estado destino. PUBLICADA es publicar un borrador o reanudar una pausada; para eso se
     * consulta el estado actual. Las reglas de cada transición las valida el dominio.
     */
    @Override
    public Publicacion cambiarEstado(String userId, String publicacionId, EstadoPublicacion estado) {
        return ejecutar(() -> ModelosMapper.aModelo(transicionar(userId, publicacionId, estado)));
    }

    private PublicacionDTO transicionar(String userId, String publicacionId, EstadoPublicacion estado) {
        return switch (estado) {
            case PUBLICADA -> publicarOReanudar(userId, publicacionId);
            case PAUSADA -> publicacionService.pausar(userId, publicacionId, MOTIVO_POR_DEFECTO);
            case CERRADA -> publicacionService.cerrar(userId, publicacionId, MOTIVO_POR_DEFECTO);
            case CANCELADA -> publicacionService.cancelar(userId, publicacionId, MOTIVO_POR_DEFECTO);
            case BORRADOR -> throw new OperacionRechazadaException("Una publicación no puede volver a BORRADOR");
        };
    }

    private PublicacionDTO publicarOReanudar(String userId, String publicacionId) {
        var actual = publicacionService.verPublicacion(userId, publicacionId);
        if (EstadoPublicacion.PAUSADA.name().equals(actual.estado())) {
            return publicacionService.reanudar(userId, publicacionId);
        }
        return publicacionService.publicar(userId, publicacionId);
    }

    @Override
    public Publicacion agregarFranja(String userId, String publicacionId, LocalDateTime inicio, LocalDateTime fin) {
        return ejecutar(() -> ModelosMapper.aModelo(publicacionService.agregarFranja(userId, publicacionId,
                ModelosMapper.aInstante(inicio), ModelosMapper.aInstante(fin))));
    }

    @Override
    public Publicacion agregarDescuentoPorcentual(String userId, String publicacionId, int porcentaje) {
        return agregarPromocion(userId, publicacionId, new PromocionDTO(PromocionDTO.Tipo.PORCENTUAL, porcentaje));
    }

    @Override
    public Publicacion agregarDescuentoFijo(String userId, String publicacionId, int skillCoins) {
        return agregarPromocion(userId, publicacionId, new PromocionDTO(PromocionDTO.Tipo.FIJO, skillCoins));
    }

    private Publicacion agregarPromocion(String userId, String publicacionId, PromocionDTO promocion) {
        return ejecutar(() -> ModelosMapper.aModelo(
                publicacionService.agregarPromocion(userId, publicacionId, promocion)));
    }
}
