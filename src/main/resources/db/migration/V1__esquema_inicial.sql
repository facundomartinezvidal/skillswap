-- Esquema inicial de SkillSwap, alineado con el dominio (domain/*).
-- Las personas se identifican por el user_id del servicio de identidad (no hay un id de perfil aparte).
-- Los ids de los agregados son UUID guardados como texto (VARCHAR(36)), para poder leerlos en la base.
-- Las fechas son Instant del dominio, guardadas en UTC con microsegundos (DATETIME(6)).
-- Los estados se guardan por nombre ('PUBLICADA', 'CONFIRMADA', ...), igual que EstadoX.nombre().

-- ---------------------------------------------------------------------------------------------
-- Perfiles
-- ---------------------------------------------------------------------------------------------

CREATE TABLE perfiles (
    user_id VARCHAR(128) NOT NULL,
    tipo VARCHAR(16) NOT NULL,
    nombre_visible VARCHAR(160) NOT NULL,
    biografia VARCHAR(2000) NOT NULL,
    zona_horaria VARCHAR(80) NOT NULL,
    canal_notificacion VARCHAR(16) NOT NULL,
    -- Reputación acumulada (Reputacion: suma y cantidad), para filtrar y ordenar sin leer calificaciones.
    reputacion_suma BIGINT NOT NULL,
    reputacion_cantidad INT NOT NULL,
    activo BOOLEAN NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT ck_perfiles_tipo CHECK (tipo IN ('PROFESIONAL', 'ORGANIZACION')),
    CONSTRAINT ck_perfiles_canal CHECK (canal_notificacion IN ('EMAIL', 'PUSH', 'INTERNA')),
    CONSTRAINT ck_perfiles_reputacion CHECK (reputacion_cantidad >= 0
        AND reputacion_suma BETWEEN reputacion_cantidad * 1 AND reputacion_cantidad * 5)
) ENGINE=InnoDB;

CREATE TABLE perfil_habilidades (
    user_id VARCHAR(128) NOT NULL,
    habilidad VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, habilidad),
    CONSTRAINT fk_habilidad_perfil FOREIGN KEY (user_id) REFERENCES perfiles (user_id),
    -- Para la búsqueda por habilidad del autor.
    INDEX ix_habilidades_habilidad (habilidad)
) ENGINE=InnoDB;

CREATE TABLE perfil_intereses (
    user_id VARCHAR(128) NOT NULL,
    interes VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, interes),
    CONSTRAINT fk_interes_perfil FOREIGN KEY (user_id) REFERENCES perfiles (user_id)
) ENGINE=InnoDB;

CREATE TABLE perfil_disponibilidad (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(128) NOT NULL,
    -- DayOfWeek.getValue(): 1 = lunes ... 7 = domingo.
    dia_semana INT NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_disponibilidad_perfil FOREIGN KEY (user_id) REFERENCES perfiles (user_id),
    CONSTRAINT ck_disponibilidad_dia CHECK (dia_semana BETWEEN 1 AND 7),
    -- Sin UNIQUE: que no se superpongan lo garantiza el dominio (Perfil.agregarDisponibilidad).
    CONSTRAINT ck_disponibilidad_horario CHECK (hora_inicio < hora_fin)
) ENGINE=InnoDB;

CREATE TABLE historial_perfiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(128) NOT NULL,
    desde VARCHAR(16) NOT NULL,
    hacia VARCHAR(16) NOT NULL,
    fecha DATETIME(6) NOT NULL,
    actor_id VARCHAR(128) NOT NULL,
    motivo VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_historial_perfil FOREIGN KEY (user_id) REFERENCES perfiles (user_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------------------------
-- Publicaciones
-- ---------------------------------------------------------------------------------------------

CREATE TABLE publicaciones (
    id VARCHAR(36) NOT NULL,
    -- Orden de alta, para listar sin depender de la fecha (el dominio no guarda cuándo se creó).
    secuencia BIGINT NOT NULL AUTO_INCREMENT,
    autor_id VARCHAR(128) NOT NULL,
    tipo VARCHAR(16) NOT NULL,
    -- Un BORRADOR puede estar incompleto: por eso título, categoría, modalidad, duración y precio admiten nulos.
    titulo VARCHAR(180) NULL,
    descripcion VARCHAR(4000) NULL,
    categoria VARCHAR(100) NULL,
    modalidad VARCHAR(16) NULL,
    duracion_minutos INT NULL,
    cupo INT NOT NULL,
    precio_base BIGINT NULL,
    -- Denormalizado: precioFinal() con las promociones aplicadas, calculado por el dominio al guardar.
    -- Permite filtrar y ordenar por lo que paga el cliente en SQL, sin traer las promociones.
    precio_final BIGINT NULL,
    -- Denormalizado: la última vez que pasó a PUBLICADA (según el historial), para ordenar por más reciente.
    publicada_en DATETIME(6) NULL,
    estado VARCHAR(16) NOT NULL,
    motivo_cancelacion VARCHAR(500) NULL,
    -- Bloqueo optimista: dos aceptaciones simultáneas no pueden tomar el mismo último lugar.
    version BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_publicaciones_secuencia UNIQUE (secuencia),
    CONSTRAINT fk_publicacion_autor FOREIGN KEY (autor_id) REFERENCES perfiles (user_id),
    CONSTRAINT ck_publicacion_tipo CHECK (tipo IN ('OFERTA', 'DESAFIO')),
    CONSTRAINT ck_publicacion_modalidad CHECK (modalidad IN ('REMOTA', 'PRESENCIAL', 'HIBRIDA')),
    CONSTRAINT ck_publicacion_estado CHECK (estado IN ('BORRADOR', 'PUBLICADA', 'PAUSADA', 'CERRADA', 'CANCELADA')),
    CONSTRAINT ck_publicacion_cupo CHECK (cupo >= 0),
    CONSTRAINT ck_publicacion_precio CHECK (precio_base >= 0 AND precio_final >= 0),
    -- Búsqueda: siempre filtra por estado y suele filtrar por categoría, modalidad y precio.
    INDEX ix_publicaciones_busqueda (estado, categoria, modalidad, precio_final),
    INDEX ix_publicaciones_precio (estado, precio_final),
    INDEX ix_publicaciones_recientes (estado, publicada_en),
    -- Panel del autor y perfil público.
    INDEX ix_publicaciones_autor (autor_id, estado, secuencia)
) ENGINE=InnoDB;

CREATE TABLE publicacion_etiquetas (
    publicacion_id VARCHAR(36) NOT NULL,
    etiqueta VARCHAR(100) NOT NULL,
    PRIMARY KEY (publicacion_id, etiqueta),
    CONSTRAINT fk_etiqueta_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicaciones (id),
    INDEX ix_etiquetas_etiqueta (etiqueta)
) ENGINE=InnoDB;

CREATE TABLE franjas_disponibilidad (
    id VARCHAR(36) NOT NULL,
    publicacion_id VARCHAR(36) NOT NULL,
    inicio DATETIME(6) NOT NULL,
    fin DATETIME(6) NOT NULL,
    -- El cupo es por franja: cuántos lugares de la franja ya se tomaron.
    reservados INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_franja_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicaciones (id),
    CONSTRAINT ck_franja_horario CHECK (inicio < fin),
    CONSTRAINT ck_franja_reservados CHECK (reservados >= 0),
    INDEX ix_franjas_publicacion (publicacion_id, inicio)
) ENGINE=InnoDB;

CREATE TABLE promociones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    publicacion_id VARCHAR(36) NOT NULL,
    -- Las promociones se acumulan en el orden en que se agregaron, y el orden cambia el precio final.
    orden INT NOT NULL,
    tipo VARCHAR(16) NOT NULL,
    -- Porcentaje (1 a 100) para PORCENTUAL, monto en SkillCoins para FIJO.
    valor BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_promocion_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicaciones (id),
    CONSTRAINT uq_promocion_orden UNIQUE (publicacion_id, orden),
    CONSTRAINT ck_promocion_tipo CHECK (tipo IN ('PORCENTUAL', 'FIJO')),
    CONSTRAINT ck_promocion_valor CHECK (valor > 0)
) ENGINE=InnoDB;

CREATE TABLE historial_publicaciones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    publicacion_id VARCHAR(36) NOT NULL,
    desde VARCHAR(16) NOT NULL,
    hacia VARCHAR(16) NOT NULL,
    fecha DATETIME(6) NOT NULL,
    actor_id VARCHAR(128) NOT NULL,
    motivo VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_historial_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicaciones (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------------------------
-- Contrataciones (incluye la solicitud, la reprogramación pendiente, el reintegro y las calificaciones)
-- ---------------------------------------------------------------------------------------------

CREATE TABLE contrataciones (
    id VARCHAR(36) NOT NULL,
    secuencia BIGINT NOT NULL AUTO_INCREMENT,
    publicacion_id VARCHAR(36) NOT NULL,
    cliente_id VARCHAR(128) NOT NULL,
    proveedor_id VARCHAR(128) NOT NULL,
    -- Solicitud: lo que pidió el cliente, con la franja que eligió originalmente.
    mensaje VARCHAR(2000) NULL,
    objetivo VARCHAR(500) NOT NULL,
    franja_solicitada_id VARCHAR(36) NOT NULL,
    -- Franja e inicio actuales: cambian si se acepta una reprogramación.
    franja_id VARCHAR(36) NOT NULL,
    inicio_sesion DATETIME(6) NOT NULL,
    precio_acordado BIGINT NOT NULL,
    estado VARCHAR(16) NOT NULL,
    -- Propuesta de reprogramación pendiente (todo nulo si no hay).
    propuesta_por VARCHAR(128) NULL,
    propuesta_franja_id VARCHAR(36) NULL,
    propuesta_inicio DATETIME(6) NULL,
    -- Reintegro calculado al cancelar (nulo si no se canceló).
    reintegro_cliente BIGINT NULL,
    reintegro_proveedor BIGINT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_contrataciones_secuencia UNIQUE (secuencia),
    CONSTRAINT fk_contratacion_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicaciones (id),
    CONSTRAINT fk_contratacion_cliente FOREIGN KEY (cliente_id) REFERENCES perfiles (user_id),
    CONSTRAINT fk_contratacion_proveedor FOREIGN KEY (proveedor_id) REFERENCES perfiles (user_id),
    CONSTRAINT fk_contratacion_franja FOREIGN KEY (franja_id) REFERENCES franjas_disponibilidad (id),
    CONSTRAINT ck_contratacion_estado CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'CONFIRMADA', 'EN_CURSO',
        'COMPLETADA', 'RECHAZADA', 'CANCELADA')),
    CONSTRAINT ck_contratacion_partes CHECK (cliente_id <> proveedor_id),
    CONSTRAINT ck_contratacion_precio CHECK (precio_acordado >= 0),
    CONSTRAINT ck_contratacion_propuesta CHECK ((propuesta_por IS NULL AND propuesta_franja_id IS NULL
        AND propuesta_inicio IS NULL) OR (propuesta_por IS NOT NULL AND propuesta_franja_id IS NOT NULL
        AND propuesta_inicio IS NOT NULL)),
    CONSTRAINT ck_contratacion_reintegro CHECK ((reintegro_cliente IS NULL AND reintegro_proveedor IS NULL)
        OR (reintegro_cliente >= 0 AND reintegro_proveedor >= 0)),
    -- "Mis contrataciones" como cliente o como proveedor, de la más nueva a la más vieja.
    INDEX ix_contrataciones_cliente (cliente_id, secuencia),
    INDEX ix_contrataciones_proveedor (proveedor_id, secuencia, estado),
    INDEX ix_contrataciones_publicacion (publicacion_id, franja_id, estado)
) ENGINE=InnoDB;

CREATE TABLE historial_contrataciones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    contratacion_id VARCHAR(36) NOT NULL,
    desde VARCHAR(16) NOT NULL,
    hacia VARCHAR(16) NOT NULL,
    fecha DATETIME(6) NOT NULL,
    actor_id VARCHAR(128) NOT NULL,
    motivo VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_historial_contratacion FOREIGN KEY (contratacion_id) REFERENCES contrataciones (id),
    -- Popularidad reciente: contrataciones que pasaron a ACEPTADA desde una fecha.
    INDEX ix_historial_contratacion_hacia (hacia, fecha)
) ENGINE=InnoDB;

CREATE TABLE calificaciones (
    id VARCHAR(36) NOT NULL,
    contratacion_id VARCHAR(36) NOT NULL,
    autor_id VARCHAR(128) NOT NULL,
    destinatario_id VARCHAR(128) NOT NULL,
    puntaje INT NOT NULL,
    comentario VARCHAR(1000) NULL,
    fecha DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_calificacion_contratacion FOREIGN KEY (contratacion_id) REFERENCES contrataciones (id),
    CONSTRAINT fk_calificacion_autor FOREIGN KEY (autor_id) REFERENCES perfiles (user_id),
    CONSTRAINT fk_calificacion_destinatario FOREIGN KEY (destinatario_id) REFERENCES perfiles (user_id),
    -- Una calificación por participante y contratación, aunque dos operaciones lleguen a la vez.
    CONSTRAINT uq_calificacion_por_participante UNIQUE (contratacion_id, autor_id),
    CONSTRAINT ck_calificacion_puntaje CHECK (puntaje BETWEEN 1 AND 5),
    CONSTRAINT ck_calificacion_partes CHECK (autor_id <> destinatario_id),
    INDEX ix_calificacion_destinatario (destinatario_id, fecha)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------------------------
-- Wallets y movimientos
-- ---------------------------------------------------------------------------------------------

-- Sin FK a perfiles: la Wallet de garantía de la plataforma ('skillswap-garantia') no es de ningún perfil.
CREATE TABLE wallets (
    titular_id VARCHAR(128) NOT NULL,
    saldo BIGINT NOT NULL,
    -- Bloqueo optimista: la garantía la tocan todos los pagos, y dos pagos a la vez no pueden pisarse el saldo.
    version BIGINT NOT NULL,
    PRIMARY KEY (titular_id),
    CONSTRAINT ck_wallet_saldo CHECK (saldo >= 0)
) ENGINE=InnoDB;

CREATE TABLE movimientos_wallet (
    id VARCHAR(36) NOT NULL,
    secuencia BIGINT NOT NULL AUTO_INCREMENT,
    titular_id VARCHAR(128) NOT NULL,
    -- Nulo en las acreditaciones de demostración (bono de bienvenida y carga de saldo).
    contratacion_id VARCHAR(36) NULL,
    tipo VARCHAR(20) NOT NULL,
    importe BIGINT NOT NULL,
    saldo_resultante BIGINT NOT NULL,
    concepto VARCHAR(240) NOT NULL,
    fecha DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_movimientos_secuencia UNIQUE (secuencia),
    CONSTRAINT fk_movimiento_wallet FOREIGN KEY (titular_id) REFERENCES wallets (titular_id),
    CONSTRAINT fk_movimiento_contratacion FOREIGN KEY (contratacion_id) REFERENCES contrataciones (id),
    CONSTRAINT ck_movimiento_tipo CHECK (tipo IN ('CREDITO', 'DEBITO', 'REINTEGRO', 'ACREDITACION_DEMO')),
    CONSTRAINT ck_movimiento_importe CHECK (importe > 0),
    CONSTRAINT ck_movimiento_saldo CHECK (saldo_resultante >= 0),
    -- Impide el doble cobro: por contratación, cada Wallet tiene a lo sumo un movimiento de cada tipo.
    -- Ningún flujo de RetencionDePagos lo necesita dos veces: el cliente tiene un DEBITO (pago) y quizás un
    -- REINTEGRO (cancelación); la garantía, un CREDITO (pago) y un DEBITO (liberación o reparto, nunca los
    -- dos); el proveedor, un CREDITO (cobro al completar o cobro parcial al cancelar, nunca los dos).
    -- Las acreditaciones de demo tienen contratacion_id nulo, y MySQL no compara nulos en un UNIQUE.
    CONSTRAINT uq_movimiento_contratacion_titular_tipo UNIQUE (contratacion_id, titular_id, tipo),
    INDEX ix_movimientos_titular (titular_id, fecha, secuencia)
) ENGINE=InnoDB;

-- La Wallet de garantía existe siempre: es parte de la plataforma, no un dato de demostración.
INSERT INTO wallets (titular_id, saldo, version) VALUES ('skillswap-garantia', 0, 0);

-- ---------------------------------------------------------------------------------------------
-- Notificaciones
-- ---------------------------------------------------------------------------------------------

-- Sin FK a perfiles: la notificación es genérica y solo conoce el userId del destinatario.
CREATE TABLE notificaciones (
    id VARCHAR(36) NOT NULL,
    secuencia BIGINT NOT NULL AUTO_INCREMENT,
    destinatario_id VARCHAR(128) NOT NULL,
    tipo VARCHAR(60) NOT NULL,
    titulo VARCHAR(180) NOT NULL,
    detalle VARCHAR(1000) NOT NULL,
    fecha DATETIME(6) NOT NULL,
    leida BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_notificaciones_secuencia UNIQUE (secuencia),
    INDEX ix_notificaciones_bandeja (destinatario_id, fecha, secuencia),
    INDEX ix_notificaciones_no_leidas (destinatario_id, leida)
) ENGINE=InnoDB;
