package ar.edu.uade.skillswap.application.contratacion.dto;

/** Lo que el cliente indica al solicitar: publicación, franja elegida, mensaje y objetivo. */
public record SolicitarContratacionCommand(String clienteId, String publicacionId, String franjaId, String mensaje,
                                           String objetivo) {
}
