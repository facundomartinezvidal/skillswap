package ar.edu.uade.skillswap.domain.contratacion.strategy;

import ar.edu.uade.skillswap.domain.contratacion.Reintegro;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;

import java.time.Instant;

/** Patrón Strategy: cómo se reparte el precio pagado cuando se cancela una contratación confirmada. */
public interface PoliticaReintegro {

    Reintegro calcular(SkillCoins precio, Instant inicioSesion, Instant ahora);
}
