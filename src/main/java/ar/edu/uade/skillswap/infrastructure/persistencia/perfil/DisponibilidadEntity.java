package ar.edu.uade.skillswap.infrastructure.persistencia.perfil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalTime;

/** Tabla {@code perfil_disponibilidad}: un horario semanal del perfil. {@code diaSemana} va de 1 (lunes) a 7. */
@Entity
@Table(name = "perfil_disponibilidad")
public class DisponibilidadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    PerfilEntity perfil;

    @Column(name = "dia_semana", nullable = false)
    int diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    LocalTime horaFin;

    protected DisponibilidadEntity() {
    }

    DisponibilidadEntity(PerfilEntity perfil) {
        this.perfil = perfil;
    }
}
