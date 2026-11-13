CREATE TABLE perfiles (
    id VARCHAR(64) NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    nombre_visible VARCHAR(160) NOT NULL,
    biografia TEXT NOT NULL,
    zona_horaria VARCHAR(80) NOT NULL,
    canal_notificacion VARCHAR(24) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_perfiles_user UNIQUE (user_id),
    CONSTRAINT ck_perfiles_canal CHECK (canal_notificacion IN ('EMAIL', 'PUSH', 'INTERNA'))
) ENGINE=InnoDB;

CREATE TABLE perfil_habilidades (
    perfil_id VARCHAR(64) NOT NULL,
    habilidad VARCHAR(100) NOT NULL,
    PRIMARY KEY (perfil_id, habilidad),
    CONSTRAINT fk_habilidad_perfil FOREIGN KEY (perfil_id) REFERENCES perfiles (id)
) ENGINE=InnoDB;

CREATE TABLE perfil_intereses (
    perfil_id VARCHAR(64) NOT NULL,
    interes VARCHAR(100) NOT NULL,
    PRIMARY KEY (perfil_id, interes),
    CONSTRAINT fk_interes_perfil FOREIGN KEY (perfil_id) REFERENCES perfiles (id)
) ENGINE=InnoDB;

CREATE TABLE perfil_disponibilidad (
    id BIGINT NOT NULL AUTO_INCREMENT,
    perfil_id VARCHAR(64) NOT NULL,
    dia_semana TINYINT NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_disponibilidad_perfil FOREIGN KEY (perfil_id) REFERENCES perfiles (id),
    CONSTRAINT ck_disponibilidad_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_disponibilidad_horario CHECK (hora_inicio < hora_fin),
    CONSTRAINT uq_disponibilidad_perfil UNIQUE (perfil_id, dia_semana, hora_inicio, hora_fin)
) ENGINE=InnoDB;

CREATE TABLE categorias (
    id VARCHAR(64) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_categorias_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE publicaciones (
    id VARCHAR(64) NOT NULL,
    autor_perfil_id VARCHAR(64) NOT NULL,
    categoria_id VARCHAR(64) NOT NULL,
    tipo VARCHAR(16) NOT NULL,
    titulo VARCHAR(180) NOT NULL,
    descripcion TEXT NOT NULL,
    modalidad VARCHAR(16) NOT NULL,
    duracion_minutos INT NOT NULL,
    cupo INT NOT NULL,
    precio_skillcoins BIGINT NOT NULL,
    estado VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    creada_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_publicacion_autor FOREIGN KEY (autor_perfil_id) REFERENCES perfiles (id),
    CONSTRAINT fk_publicacion_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT ck_publicacion_tipo CHECK (tipo IN ('OFERTA', 'DESAFIO')),
    CONSTRAINT ck_publicacion_modalidad CHECK (modalidad IN ('REMOTA', 'PRESENCIAL')),
    CONSTRAINT ck_publicacion_estado CHECK (estado IN ('BORRADOR', 'PUBLICADA', 'PAUSADA', 'CERRADA', 'CANCELADA')),
    CONSTRAINT ck_publicacion_duracion CHECK (duracion_minutos > 0),
    CONSTRAINT ck_publicacion_cupo CHECK (cupo > 0),
    CONSTRAINT ck_publicacion_precio CHECK (precio_skillcoins >= 0),
    INDEX ix_publicaciones_busqueda (estado, categoria_id, modalidad, precio_skillcoins),
    INDEX ix_publicaciones_autor (autor_perfil_id, estado)
) ENGINE=InnoDB;

CREATE TABLE publicacion_etiquetas (
    publicacion_id VARCHAR(64) NOT NULL,
    etiqueta VARCHAR(100) NOT NULL,
    PRIMARY KEY (publicacion_id, etiqueta),
    CONSTRAINT fk_etiqueta_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicaciones (id)
) ENGINE=InnoDB;

CREATE TABLE franjas_disponibilidad (
    id BIGINT NOT NULL AUTO_INCREMENT,
    publicacion_id VARCHAR(64) NOT NULL,
    inicio DATETIME NOT NULL,
    fin DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_franja_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicaciones (id),
    CONSTRAINT ck_franja_horario CHECK (inicio < fin),
    CONSTRAINT uq_franja_publicacion UNIQUE (publicacion_id, inicio, fin),
    CONSTRAINT uq_franja_id_publicacion UNIQUE (id, publicacion_id),
    INDEX ix_franjas_inicio (publicacion_id, inicio, fin)
) ENGINE=InnoDB;

CREATE TABLE contrataciones (
    id VARCHAR(64) NOT NULL,
    publicacion_id VARCHAR(64) NOT NULL,
    cliente_perfil_id VARCHAR(64) NOT NULL,
    proveedor_perfil_id VARCHAR(64) NOT NULL,
    franja_id BIGINT NOT NULL,
    mensaje TEXT NOT NULL,
    objetivo VARCHAR(500) NOT NULL,
    precio_skillcoins BIGINT NOT NULL,
    estado VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    solicitada_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_contratacion_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicaciones (id),
    CONSTRAINT fk_contratacion_cliente FOREIGN KEY (cliente_perfil_id) REFERENCES perfiles (id),
    CONSTRAINT fk_contratacion_proveedor FOREIGN KEY (proveedor_perfil_id) REFERENCES perfiles (id),
    CONSTRAINT fk_contratacion_franja_publicacion FOREIGN KEY (franja_id, publicacion_id)
        REFERENCES franjas_disponibilidad (id, publicacion_id),
    CONSTRAINT ck_contratacion_estado CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'CONFIRMADA', 'EN_CURSO', 'COMPLETADA', 'RECHAZADA', 'CANCELADA')),
    CONSTRAINT ck_contratacion_partes CHECK (cliente_perfil_id <> proveedor_perfil_id),
    CONSTRAINT ck_contratacion_precio CHECK (precio_skillcoins >= 0),
    INDEX ix_contrataciones_proveedor_estado (proveedor_perfil_id, estado),
    INDEX ix_contrataciones_cliente_estado (cliente_perfil_id, estado),
    INDEX ix_contrataciones_reserva (publicacion_id, franja_id, estado)
) ENGINE=InnoDB;

CREATE TABLE historial_publicaciones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    publicacion_id VARCHAR(64) NOT NULL,
    estado_anterior VARCHAR(16) NOT NULL,
    estado_nuevo VARCHAR(16) NOT NULL,
    actor_user_id VARCHAR(128) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    fecha TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_historial_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicaciones (id),
    INDEX ix_historial_publicacion_fecha (publicacion_id, fecha)
) ENGINE=InnoDB;

CREATE TABLE historial_contrataciones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    contratacion_id VARCHAR(64) NOT NULL,
    estado_anterior VARCHAR(16) NOT NULL,
    estado_nuevo VARCHAR(16) NOT NULL,
    actor_user_id VARCHAR(128) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    fecha TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_historial_contratacion FOREIGN KEY (contratacion_id) REFERENCES contrataciones (id),
    INDEX ix_historial_contratacion_fecha (contratacion_id, fecha)
) ENGINE=InnoDB;

CREATE TABLE propuestas_reprogramacion (
    id VARCHAR(64) NOT NULL,
    contratacion_id VARCHAR(64) NOT NULL,
    propuesto_por_perfil_id VARCHAR(64) NOT NULL,
    respondido_por_perfil_id VARCHAR(64) NULL,
    inicio_propuesto DATETIME NOT NULL,
    fin_propuesto DATETIME NOT NULL,
    estado VARCHAR(12) NOT NULL,
    creada_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    respondida_en TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reprogramacion_contratacion FOREIGN KEY (contratacion_id) REFERENCES contrataciones (id),
    CONSTRAINT fk_reprogramacion_proponente FOREIGN KEY (propuesto_por_perfil_id) REFERENCES perfiles (id),
    CONSTRAINT fk_reprogramacion_respondedor FOREIGN KEY (respondido_por_perfil_id) REFERENCES perfiles (id),
    CONSTRAINT ck_reprogramacion_horario CHECK (inicio_propuesto < fin_propuesto),
    CONSTRAINT ck_reprogramacion_estado CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA')),
    INDEX ix_reprogramacion_contratacion (contratacion_id, estado, creada_en)
) ENGINE=InnoDB;

CREATE TABLE wallets (
    perfil_id VARCHAR(64) NOT NULL,
    saldo_skillcoins BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (perfil_id),
    CONSTRAINT fk_wallet_perfil FOREIGN KEY (perfil_id) REFERENCES perfiles (id),
    CONSTRAINT ck_wallet_saldo CHECK (saldo_skillcoins >= 0)
) ENGINE=InnoDB;

CREATE TABLE movimientos_wallet (
    id VARCHAR(64) NOT NULL,
    wallet_perfil_id VARCHAR(64) NOT NULL,
    contratacion_id VARCHAR(64) NULL,
    tipo VARCHAR(20) NOT NULL,
    importe_skillcoins BIGINT NOT NULL,
    saldo_resultante BIGINT NOT NULL,
    concepto VARCHAR(240) NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_movimiento_wallet FOREIGN KEY (wallet_perfil_id) REFERENCES wallets (perfil_id),
    CONSTRAINT fk_movimiento_contratacion FOREIGN KEY (contratacion_id) REFERENCES contrataciones (id),
    CONSTRAINT ck_movimiento_tipo CHECK (tipo IN ('CREDITO', 'DEBITO', 'REINTEGRO', 'ACREDITACION_DEMO')),
    CONSTRAINT ck_movimiento_saldo CHECK (saldo_resultante >= 0),
    UNIQUE INDEX uq_movimiento_contrato_wallet_tipo (contratacion_id, wallet_perfil_id, tipo),
    INDEX ix_movimiento_wallet_fecha (wallet_perfil_id, creado_en)
) ENGINE=InnoDB;

CREATE TABLE calificaciones (
    id VARCHAR(64) NOT NULL,
    contratacion_id VARCHAR(64) NOT NULL,
    autor_perfil_id VARCHAR(64) NOT NULL,
    destinatario_perfil_id VARCHAR(64) NOT NULL,
    puntaje TINYINT NOT NULL,
    comentario VARCHAR(1000) NOT NULL,
    creada_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_calificacion_contratacion FOREIGN KEY (contratacion_id) REFERENCES contrataciones (id),
    CONSTRAINT fk_calificacion_autor FOREIGN KEY (autor_perfil_id) REFERENCES perfiles (id),
    CONSTRAINT fk_calificacion_destinatario FOREIGN KEY (destinatario_perfil_id) REFERENCES perfiles (id),
    CONSTRAINT uq_calificacion_por_participante UNIQUE (contratacion_id, autor_perfil_id),
    CONSTRAINT ck_calificacion_puntaje CHECK (puntaje BETWEEN 1 AND 5),
    CONSTRAINT ck_calificacion_partes CHECK (autor_perfil_id <> destinatario_perfil_id),
    INDEX ix_calificacion_destinatario (destinatario_perfil_id, creada_en)
) ENGINE=InnoDB;

CREATE TABLE notificaciones (
    id VARCHAR(64) NOT NULL,
    destinatario_user_id VARCHAR(128) NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    titulo VARCHAR(180) NOT NULL,
    detalle VARCHAR(1000) NOT NULL,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    creada_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX ix_notificaciones_usuario_fecha (destinatario_user_id, creada_en),
    INDEX ix_notificaciones_usuario_leida (destinatario_user_id, leida)
) ENGINE=InnoDB;
