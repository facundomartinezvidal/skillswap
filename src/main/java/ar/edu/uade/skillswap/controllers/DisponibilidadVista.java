package ar.edu.uade.skillswap.controllers;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Una franja de la disponibilidad semanal del perfil. {@code Models.Perfil} no la tiene. */
public record DisponibilidadVista(DayOfWeek dia, LocalTime inicio, LocalTime fin) {
}
