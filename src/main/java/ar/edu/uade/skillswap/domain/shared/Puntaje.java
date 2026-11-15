package ar.edu.uade.skillswap.domain.shared;

/** Puntaje de una calificación: un entero de 1 a 5. */
public record Puntaje(int valor) {

    public static final int MINIMO = 1;
    public static final int MAXIMO = 5;

    public Puntaje {
        if (valor < MINIMO || valor > MAXIMO) {
            throw new IllegalArgumentException(
                    "El puntaje tiene que estar entre " + MINIMO + " y " + MAXIMO + ": " + valor);
        }
    }
}
