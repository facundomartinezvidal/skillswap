package ar.edu.uade.skillswap.domain.wallet;

import ar.edu.uade.skillswap.domain.contratacion.ContratacionId;
import ar.edu.uade.skillswap.domain.shared.SkillCoins;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MovimientoTest {

    private static final Instant AHORA = Instant.parse("2026-11-14T12:00:00Z");
    private static final ContratacionId CONTRATACION = ContratacionId.generate();

    private static Movimiento movimiento(TipoMovimiento tipo, long importe) {
        return new Movimiento(MovimientoId.generate(), tipo, new SkillCoins(importe), new SkillCoins(500),
                "Pago", CONTRATACION, AHORA);
    }

    @Test
    void importeConSignoEsNegativoSoloParaLosDebitos() {
        assertEquals(-100, movimiento(TipoMovimiento.DEBITO, 100).importeConSigno());
        assertEquals(100, movimiento(TipoMovimiento.CREDITO, 100).importeConSigno());
        assertEquals(100, movimiento(TipoMovimiento.REINTEGRO, 100).importeConSigno());
        assertEquals(100, movimiento(TipoMovimiento.ACREDITACION_DEMO, 100).importeConSigno());
    }

    @Test
    void rechazaImporteCero() {
        assertThrows(IllegalArgumentException.class, () -> movimiento(TipoMovimiento.CREDITO, 0));
    }

    @Test
    void exigeConceptoYFecha() {
        var id = MovimientoId.generate();
        var importe = new SkillCoins(10);

        assertThrows(IllegalArgumentException.class, () -> new Movimiento(id, TipoMovimiento.CREDITO, importe,
                importe, " ", CONTRATACION, AHORA));
        assertThrows(IllegalArgumentException.class, () -> new Movimiento(id, TipoMovimiento.CREDITO, importe,
                importe, null, CONTRATACION, AHORA));
        assertThrows(NullPointerException.class, () -> new Movimiento(id, TipoMovimiento.CREDITO, importe,
                importe, "Pago", CONTRATACION, null));
    }

    @Test
    void exigeIdTipoImporteYSaldoResultante() {
        var id = MovimientoId.generate();
        var importe = new SkillCoins(10);

        assertThrows(NullPointerException.class, () -> new Movimiento(null, TipoMovimiento.CREDITO, importe,
                importe, "Pago", CONTRATACION, AHORA));
        assertThrows(NullPointerException.class, () -> new Movimiento(id, null, importe,
                importe, "Pago", CONTRATACION, AHORA));
        assertThrows(NullPointerException.class, () -> new Movimiento(id, TipoMovimiento.CREDITO, null,
                importe, "Pago", CONTRATACION, AHORA));
        assertThrows(NullPointerException.class, () -> new Movimiento(id, TipoMovimiento.CREDITO, importe,
                null, "Pago", CONTRATACION, AHORA));
    }

    @Test
    void laContratacionEsOpcionalParaLasAcreditacionesDeDemo() {
        var demo = new Movimiento(MovimientoId.generate(), TipoMovimiento.ACREDITACION_DEMO, new SkillCoins(10),
                new SkillCoins(10), "Demo", null, AHORA);

        assertNull(demo.contratacionId());
    }

    @Test
    void losTiposCoincidenConElCheckDelEsquema() {
        assertArrayEquals(new String[] {"CREDITO", "DEBITO", "REINTEGRO", "ACREDITACION_DEMO"},
                Arrays.stream(TipoMovimiento.values()).map(Enum::name).toArray());
    }

    @Test
    void idsNuevosSonDistintosYSeComparanPorValor() {
        assertNotEquals(MovimientoId.generate(), MovimientoId.generate());
        var uuid = UUID.randomUUID();
        assertEquals(new MovimientoId(uuid), new MovimientoId(uuid));
        assertThrows(NullPointerException.class, () -> new MovimientoId(null));
    }
}
