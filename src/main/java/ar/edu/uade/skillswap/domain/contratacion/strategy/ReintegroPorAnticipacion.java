package ar.edu.uade.skillswap.domain.contratacion.strategy;

import ar.edu.uade.skillswap.domain.contratacion.Reintegro;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Regla 5: devuelve 100 %, 50 % o 0 % al cliente según la anticipación (inicio de la sesión menos
 * ahora). Los umbrales son configurables e inclusivos: exactamente {@code para100} devuelve el 100 %.
 * Lo que no vuelve al cliente lo cobra el proveedor. Con un precio impar, en el 50 % el cliente recibe
 * la mitad redondeada hacia abajo y el proveedor el resto, así la suma siempre es el precio.
 */
public final class ReintegroPorAnticipacion implements PoliticaReintegro {

    private final Duration para100;
    private final Duration para50;

    public ReintegroPorAnticipacion(Duration para100, Duration para50) {
        Objects.requireNonNull(para100, "El umbral del 100 % es obligatorio");
        Objects.requireNonNull(para50, "El umbral del 50 % es obligatorio");
        if (para50.isNegative()) {
            throw new IllegalArgumentException("El umbral del 50 % no puede ser negativo");
        }
        if (para100.compareTo(para50) <= 0) {
            throw new IllegalArgumentException("El umbral del 100 % tiene que ser mayor que el del 50 %");
        }
        this.para100 = para100;
        this.para50 = para50;
    }

    /** 100 % con 48 h o más de anticipación, 50 % con 24 h o más, 0 % con menos. */
    public static ReintegroPorAnticipacion conUmbralesPorDefecto() {
        return new ReintegroPorAnticipacion(Duration.ofHours(48), Duration.ofHours(24));
    }

    @Override
    public Reintegro calcular(SkillCoins precio, Instant inicioSesion, Instant ahora) {
        Objects.requireNonNull(precio, "El precio es obligatorio");
        Objects.requireNonNull(inicioSesion, "El inicio de la sesión es obligatorio");
        Objects.requireNonNull(ahora, "El momento de la cancelación es obligatorio");
        var anticipacion = Duration.between(ahora, inicioSesion);
        long alCliente;
        if (anticipacion.compareTo(para100) >= 0) {
            alCliente = precio.valor();
        } else if (anticipacion.compareTo(para50) >= 0) {
            alCliente = precio.valor() / 2;
        } else {
            alCliente = 0;
        }
        return new Reintegro(new SkillCoins(alCliente), new SkillCoins(precio.valor() - alCliente));
    }

    public Duration para100() {
        return para100;
    }

    public Duration para50() {
        return para50;
    }
}
