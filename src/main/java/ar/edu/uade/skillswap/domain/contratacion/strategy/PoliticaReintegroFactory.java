package ar.edu.uade.skillswap.domain.contratacion.strategy;

import ar.edu.uade.skillswap.domain.contratacion.Contratacion;
import ar.edu.uade.skillswap.domain.contratacion.exception.AccionNoPermitidaException;

import java.util.Objects;

/**
 * Simple Factory: elige la política según quién cancela. Si cancela el proveedor, el cliente
 * recupera todo; si cancela el cliente, depende de la anticipación configurada.
 */
public final class PoliticaReintegroFactory {

    private final ReintegroPorAnticipacion porAnticipacion;
    private final ReintegroTotal total = new ReintegroTotal();

    public PoliticaReintegroFactory(ReintegroPorAnticipacion porAnticipacion) {
        this.porAnticipacion = Objects.requireNonNull(porAnticipacion, "La política por anticipación es obligatoria");
    }

    public PoliticaReintegro paraCancelacion(Contratacion contratacion, String canceladorId) {
        Objects.requireNonNull(contratacion, "La contratación es obligatoria");
        if (contratacion.proveedorId().equals(canceladorId)) {
            return total;
        }
        if (contratacion.clienteId().equals(canceladorId)) {
            return porAnticipacion;
        }
        throw new AccionNoPermitidaException(canceladorId, "cancelar");
    }
}
