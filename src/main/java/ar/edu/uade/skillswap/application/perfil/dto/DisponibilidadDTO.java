package ar.edu.uade.skillswap.application.perfil.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DisponibilidadDTO(DayOfWeek dia, LocalTime inicio, LocalTime fin) {
}
