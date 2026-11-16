package ar.edu.uade.skillswap.application.publicacion.dto;

/** Una promoción: porcentaje de 1 a 100 o monto fijo en SkillCoins. */
public record PromocionDTO(Tipo tipo, long valor) {

    public PromocionDTO {
        if (tipo == null) {
            throw new IllegalArgumentException("La promoción necesita un tipo");
        }
        if (tipo == Tipo.PORCENTUAL && (valor < 1 || valor > 100)) {
            throw new IllegalArgumentException("El porcentaje tiene que estar entre 1 y 100");
        }
    }

    public enum Tipo { PORCENTUAL, FIJO }
}
