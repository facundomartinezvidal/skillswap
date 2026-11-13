package ar.edu.uade.skillswap.domain.contratacion.strategy;

import ar.edu.uade.skillswap.domain.contratacion.Reintegro;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;

import java.time.Instant;
import java.util.Objects;

/** Devuelve todo al cliente, sin importar la anticipación. Se usa cuando cancela el proveedor. */
public final class ReintegroTotal implements PoliticaReintegro {

    @Override
    public Reintegro calcular(SkillCoins precio, Instant inicioSesion, Instant ahora) {
        Objects.requireNonNull(precio, "El precio es obligatorio");
        return new Reintegro(precio, new SkillCoins(0));
    }
}
