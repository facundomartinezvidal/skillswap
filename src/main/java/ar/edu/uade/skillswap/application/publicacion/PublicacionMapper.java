package ar.edu.uade.skillswap.application.publicacion;

import ar.edu.uade.skillswap.application.publicacion.dto.FranjaDTO;
import ar.edu.uade.skillswap.application.publicacion.dto.PromocionDTO;
import ar.edu.uade.skillswap.application.publicacion.dto.PublicacionDTO;
import ar.edu.uade.skillswap.domain.perfil.Perfil;
import ar.edu.uade.skillswap.domain.publicacion.FranjaDisponibilidad;
import ar.edu.uade.skillswap.domain.publicacion.Publicacion;
import ar.edu.uade.skillswap.domain.publicacion.decorator.DescuentoFijo;
import ar.edu.uade.skillswap.domain.publicacion.decorator.DescuentoPorcentual;
import ar.edu.uade.skillswap.domain.publicacion.decorator.Promocion;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;

/**
 * Pasa publicaciones y promociones entre el dominio y los DTOs. No tiene lógica.
 * Es público porque también lo usan la búsqueda y el perfil público.
 */
public final class PublicacionMapper {

    private PublicacionMapper() {
    }

    /** {@code autor} puede ser nulo si el perfil no existe: el DTO sale sin nombre ni reputación. */
    public static PublicacionDTO aDTO(Publicacion p, Perfil autor) {
        var autorNombre = autor == null ? null : autor.nombreVisible();
        var reputacion = autor == null ? 0 : autor.reputacion().promedio();
        var precioBase = p.precioBase() == null ? null : p.precioBase().valor();
        var precioFinal = p.precioBase() == null ? null : p.precioFinal().valor();
        return new PublicacionDTO(p.id().valor().toString(), p.autorId(), autorNombre, reputacion, p.tipo(),
                p.titulo(), p.descripcion(), p.categoria(), p.modalidad(), p.duracionEstimada(), p.cupo(),
                precioBase, precioFinal, p.etiquetas(), p.estado().nombre(), p.motivoCancelacion(),
                p.franjas().stream().map(f -> aDTO(f, p.cupo())).toList(),
                p.promociones().stream().map(PublicacionMapper::aDTO).toList());
    }

    public static Promocion aPromocion(PromocionDTO promocion) {
        return switch (promocion.tipo()) {
            case PORCENTUAL -> new DescuentoPorcentual(Math.toIntExact(promocion.valor()));
            case FIJO -> new DescuentoFijo(new SkillCoins(promocion.valor()));
        };
    }

    private static PromocionDTO aDTO(Promocion promocion) {
        if (promocion instanceof DescuentoPorcentual porcentual) {
            return new PromocionDTO(PromocionDTO.Tipo.PORCENTUAL, porcentual.porcentaje());
        }
        var fijo = (DescuentoFijo) promocion;
        return new PromocionDTO(PromocionDTO.Tipo.FIJO, fijo.monto().valor());
    }

    private static FranjaDTO aDTO(FranjaDisponibilidad franja, int cupo) {
        return new FranjaDTO(franja.id().valor().toString(), franja.inicio(), franja.fin(),
                Math.max(0, cupo - franja.reservados()));
    }
}
