package ar.edu.uade.skillswap.domain.shared;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class PaginaTest {

    private static final List<Integer> DIEZ = IntStream.rangeClosed(1, 10).boxed().toList();

    @Test
    void laPrimeraPaginaTraeLosPrimerosElementosYElTotal() {
        var pagina = Pagina.de(DIEZ, PaginaSolicitada.primera(4));

        assertEquals(List.of(1, 2, 3, 4), pagina.elementos());
        assertEquals(0, pagina.numero());
        assertEquals(4, pagina.tamanio());
        assertEquals(10, pagina.total());
        assertEquals(3, pagina.totalPaginas());
    }

    @Test
    void laUltimaPaginaPuedeTenerMenosElementos() {
        var pagina = Pagina.de(DIEZ, new PaginaSolicitada(2, 4));

        assertEquals(List.of(9, 10), pagina.elementos());
        assertEquals(10, pagina.total());
    }

    @Test
    void unaPaginaDespuesDelFinalVieneVaciaPeroConElTotal() {
        var pagina = Pagina.de(DIEZ, new PaginaSolicitada(5, 4));

        assertTrue(pagina.elementos().isEmpty());
        assertEquals(10, pagina.total());
    }

    @Test
    void unListadoVacioTieneCeroPaginas() {
        var pagina = Pagina.de(List.<Integer>of(), PaginaSolicitada.primera(5));

        assertTrue(pagina.elementos().isEmpty());
        assertEquals(0, pagina.totalPaginas());
    }

    @Test
    void mapTransformaLosElementosYConservaLosDatosDeLaPagina() {
        var pagina = Pagina.de(DIEZ, new PaginaSolicitada(1, 3)).map(numero -> "n" + numero);

        assertEquals(List.of("n4", "n5", "n6"), pagina.elementos());
        assertEquals(1, pagina.numero());
        assertEquals(3, pagina.tamanio());
        assertEquals(10, pagina.total());
    }

    @Test
    void losElementosNoSePuedenModificar() {
        var pagina = Pagina.de(DIEZ, PaginaSolicitada.primera(3));

        assertThrows(UnsupportedOperationException.class, () -> pagina.elementos().add(99));
    }

    @Test
    void desdeEsLaPosicionDelPrimerElemento() {
        assertEquals(0, PaginaSolicitada.primera(20).desde());
        assertEquals(40, new PaginaSolicitada(2, 20).desde());
    }

    @Test
    void rechazaNumeroNegativoYTamanioMenorAUno() {
        assertThrows(IllegalArgumentException.class, () -> new PaginaSolicitada(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> new PaginaSolicitada(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Pagina<>(List.of(), 0, 10, -1));
    }
}
