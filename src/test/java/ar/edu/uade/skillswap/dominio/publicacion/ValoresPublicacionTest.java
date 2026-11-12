package ar.edu.uade.skillswap.dominio.publicacion;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ValoresPublicacionTest {

    @Test
    void skillCoinsAceptaCeroYPositivos() {
        assertEquals(0, new SkillCoins(0).valor());
        assertEquals(150, new SkillCoins(150).valor());
    }

    @Test
    void skillCoinsRechazaNegativos() {
        assertThrows(IllegalArgumentException.class, () -> new SkillCoins(-1));
    }

    @Test
    void skillCoinsSeComparaPorValor() {
        assertEquals(new SkillCoins(10), new SkillCoins(10));
    }

    @Test
    void idsNuevosSonDistintosYSeComparanPorValor() {
        assertNotEquals(PublicacionId.nueva(), PublicacionId.nueva());
        assertNotEquals(FranjaId.nueva(), FranjaId.nueva());
        var uuid = UUID.randomUUID();
        assertEquals(new PublicacionId(uuid), new PublicacionId(uuid));
        assertEquals(new FranjaId(uuid), new FranjaId(uuid));
    }

    @Test
    void idsRechazanNulo() {
        assertThrows(NullPointerException.class, () -> new PublicacionId(null));
        assertThrows(NullPointerException.class, () -> new FranjaId(null));
    }

    @Test
    void modalidadDelDominioIncluyeHibrida() {
        assertEquals(Modalidad.HIBRIDA, Modalidad.valueOf("HIBRIDA"));
        assertEquals(3, Modalidad.values().length);
    }

    @Test
    void tipoPublicacionEsOfertaODesafio() {
        assertArrayEquals(new TipoPublicacion[]{TipoPublicacion.OFERTA, TipoPublicacion.DESAFIO},
                TipoPublicacion.values());
    }
}
