package ar.edu.uade.skillswap.domain.contratacion;

import ar.edu.uade.skillswap.domain.shared.SkillCoins;

import java.util.Objects;

/** Cómo se reparte lo pagado al cancelar: lo que vuelve al cliente y lo que cobra el proveedor. */
public record Reintegro(SkillCoins alCliente, SkillCoins alProveedor) {

    public Reintegro {
        Objects.requireNonNull(alCliente, "El monto para el cliente es obligatorio");
        Objects.requireNonNull(alProveedor, "El monto para el proveedor es obligatorio");
    }

    /** Sin movimientos: no se había pagado nada. */
    public static Reintegro cero() {
        return new Reintegro(new SkillCoins(0), new SkillCoins(0));
    }
}
