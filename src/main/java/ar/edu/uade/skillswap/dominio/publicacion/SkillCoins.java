package ar.edu.uade.skillswap.dominio.publicacion;

public record SkillCoins(long valor) {

    public SkillCoins {
        if (valor < 0) {
            throw new IllegalArgumentException("Un monto en SkillCoins no puede ser negativo: " + valor);
        }
    }
}
