package ar.edu.uade.skillswap.application.perfil.dto;

import ar.edu.uade.skillswap.domain.perfil.TipoPerfil;

import java.time.ZoneId;

/** Datos para registrar un perfil. El userId lo entrega el servicio de identidad. */
public record CrearPerfilCommand(String userId, TipoPerfil tipo, String nombreVisible, ZoneId zonaHoraria) {
}
