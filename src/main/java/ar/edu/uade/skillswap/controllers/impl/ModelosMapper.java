package ar.edu.uade.skillswap.controllers.impl;

import ar.edu.uade.skillswap.application.contratacion.dto.ContratacionDTO;
import ar.edu.uade.skillswap.application.notificacion.dto.NotificacionDTO;
import ar.edu.uade.skillswap.application.perfil.dto.PerfilDTO;
import ar.edu.uade.skillswap.application.publicacion.dto.PublicacionDTO;
import ar.edu.uade.skillswap.application.wallet.dto.MovimientoDTO;
import ar.edu.uade.skillswap.controllers.PaginaVista;
import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;
import ar.edu.uade.skillswap.domain.shared.Pagina;
import ar.edu.uade.skillswap.domain.shared.PaginaSolicitada;
import ar.edu.uade.skillswap.model.Models;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.function.Function;

/** Pasa los DTOs de la aplicación a los modelos de vista de la cátedra ({@link Models}) y viceversa. */
final class ModelosMapper {

    /**
     * La UI de la cátedra no pagina: todos los listados piden la primera página de este tamaño, que
     * alcanza de sobra para la demo.
     */
    static final int TAMANIO_UI = 50;
    static final PaginaSolicitada PRIMERA_PAGINA = PaginaSolicitada.primera(TAMANIO_UI);

    /** Las fechas se guardan como Instant; la UI las muestra en la hora local de la máquina. */
    static final ZoneId ZONA = ZoneId.systemDefault();

    private ModelosMapper() {
    }

    /** Una página de DTOs como página de modelos de vista, con el mismo número, tamaño y total. */
    static <D, M> PaginaVista<M> aVista(Pagina<D> pagina, Function<D, M> aModelo) {
        return new PaginaVista<>(pagina.elementos().stream().map(aModelo).toList(), pagina.numero(),
                pagina.tamanio(), pagina.total());
    }

    static Models.Publicacion aModelo(PublicacionDTO p) {
        var duracion = p.duracionEstimada() == null ? 0 : (int) p.duracionEstimada().toMinutes();
        var precio = p.precioFinal() == null ? 0 : p.precioFinal().intValue();
        // Models no tiene promociones: una publicación con promoción se muestra como destacada.
        return new Models.Publicacion(p.id(), texto(p.titulo()), texto(p.descripcion()), texto(p.categoria()),
                aModelo(p.modalidad()), duracion, precio, redondear(p.reputacionAutor()), texto(p.autorNombre()),
                p.etiquetas().stream().sorted().toList(), Models.EstadoPublicacion.valueOf(p.estado()),
                !p.promociones().isEmpty());
    }

    static Models.Contratacion aModelo(ContratacionDTO c, String nombreContraparte) {
        return new Models.Contratacion(c.id(), texto(c.tituloPublicacion()), nombreContraparte,
                aFecha(c.inicioSesion()), (int) c.precioAcordado(), Models.EstadoContratacion.valueOf(c.estado()));
    }

    static Models.Perfil aModelo(PerfilDTO p) {
        var rol = p.tipo() == TipoPerfil.PROFESIONAL ? "Profesional" : "Organización";
        return new Models.Perfil(p.userId(), p.nombreVisible(), rol, texto(p.biografia()),
                p.habilidades().stream().sorted().toList(), redondear(p.reputacion()), p.cantidadCalificaciones(),
                (int) p.contratacionesCompletadas());
    }

    static Models.Movimiento aModelo(MovimientoDTO m) {
        return new Models.Movimiento(m.concepto(), aFecha(m.fecha()), (int) m.importe(), (int) m.saldoResultante());
    }

    static Models.Notificacion aModelo(NotificacionDTO n) {
        return new Models.Notificacion(n.id(), n.titulo(), n.detalle(), aFecha(n.fecha()), n.leida(), n.tipo());
    }

    static Models.Modalidad aModelo(ar.edu.uade.skillswap.domain.publicacion.Modalidad modalidad) {
        return modalidad == null ? null : Models.Modalidad.valueOf(modalidad.name());
    }

    static ar.edu.uade.skillswap.domain.publicacion.Modalidad aDominio(Models.Modalidad modalidad) {
        return modalidad == null ? null : ar.edu.uade.skillswap.domain.publicacion.Modalidad.valueOf(modalidad.name());
    }

    static LocalDateTime aFecha(Instant instante) {
        return instante == null ? null : LocalDateTime.ofInstant(instante, ZONA);
    }

    static Instant aInstante(LocalDateTime fecha) {
        return fecha == null ? null : fecha.atZone(ZONA).toInstant();
    }

    /** La reputación es un promedio (por ejemplo 4.666...); la UI la muestra con un decimal. */
    private static double redondear(double valor) {
        return Math.round(valor * 10) / 10.0;
    }

    private static String texto(String valor) {
        return Objects.requireNonNullElse(valor, "");
    }
}
