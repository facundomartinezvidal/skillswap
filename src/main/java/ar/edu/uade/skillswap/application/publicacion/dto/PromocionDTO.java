package ar.edu.uade.skillswap.application.publicacion.dto;

/** Una promoción: porcentaje de 1 a 100 o monto fijo en SkillCoins. */
public record PromocionDTO(Tipo tipo, long valor) {

    public enum Tipo { PORCENTUAL, FIJO }
}
