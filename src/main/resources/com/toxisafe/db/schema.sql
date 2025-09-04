PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS USUARIO (
       id_usuario TEXT PRIMARY KEY,
       nombre_usuario TEXT NOT NULL,
       rol_usuario TEXT NOT NULL,
       username_usuario TEXT UNIQUE NOT NULL,
       password_usuario TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS BROTE (
       id_brote TEXT PRIMARY KEY,
       creador_brote TEXT NOT NULL,
       responsable_brote TEXT NOT NULL,
       fech_ini_brote TEXT NOT NULL, -- Formato 'YYYY-MM-DD'
       nombre_brote TEXT NOT NULL,
       estado_brote TEXT NOT NULL DEFAULT 'ACTIVO' CHECK (estado_brote IN ('ACTIVO','CERRADO')),
       fecha_cierre_brote TEXT,
       FOREIGN KEY (creador_brote) REFERENCES USUARIO(id_usuario),
       FOREIGN KEY (responsable_brote) REFERENCES USUARIO(id_usuario)
);


CREATE TABLE IF NOT EXISTS BROTE_ENCUESTADOR (
       id_brote_encuestador TEXT PRIMARY KEY,
       id_brote TEXT NOT NULL,
       id_usuario TEXT NOT NULL,
       FOREIGN KEY (id_brote) REFERENCES BROTE(id_brote),
       FOREIGN KEY (id_usuario) REFERENCES USUARIO(id_usuario),
       UNIQUE (id_brote, id_usuario) -- Un encuestador solo puede ser asignado una vez por brote
);

CREATE TABLE IF NOT EXISTS PERSONA_EXPUESTA (
        id_expuesto TEXT PRIMARY KEY,
        id_brote TEXT NOT NULL,
        nombre_expuesto TEXT NOT NULL,
        apellido_expuesto TEXT NOT NULL,
        tfno1_expuesto INTEGER,
        tfno2_expuesto TEXT,
        nhusa_expuesto TEXT,
        tipo_documento_expuesto TEXT,
        num_documento_expuesto TEXT,
        sexo_expuesto TEXT,
        edad_expuesto INTEGER,
        fecha_nacimiento_expuesto TEXT,
        direccion_expuesto TEXT,
        centro_salud_expuesto TEXT,
        profesion_expuesto TEXT,
        manipulador_expuesto INTEGER,
        grupo_expuesto TEXT,
        enfermo_expuesto INTEGER,
        atencion_medica_expuesto INTEGER,
        atencion_hospitalaria_expuesto INTEGER,
        fecha_atencion_medica_expuesto TEXT,
        lugar_atencion_medica_expuesto TEXT,
        evolucion_expuesto TEXT,
        tratamiento_expuesto TEXT,
        solicitud_coprocultivo_expuesto INTEGER,
        estado_coprocultivo_expuesto INTEGER,
        fecha_coprocultivo_expuesto TEXT,
        laboratorio_coprocultivo_expuesto TEXT,
        resultado_coprocultivo_expuesto TEXT,
        patogeno_coprocultivo_expuesto TEXT,
        observaciones_coprocultivo_expuesto TEXT,
        solicitud_frotis_expuesto INTEGER,
        estado_frotis_expuesto INTEGER,
        fecha_frotis_expuesto TEXT,
        laboratorio_frotis_expuesto TEXT,
        resultado_frotis_expuesto TEXT,
        patogeno_frotis_expuesto TEXT,
        observaciones_frotis_expuesto TEXT,
        FOREIGN KEY (id_brote) REFERENCES BROTE(id_brote) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS ALIMENTO (
        id_alimento TEXT PRIMARY KEY,
        id_ingesta TEXT NOT NULL,
        nombre TEXT NOT NULL,
        id_catalogo TEXT NULL,
        FOREIGN KEY (id_ingesta) REFERENCES INGESTA(id_ingesta) ON DELETE CASCADE);

CREATE TABLE IF NOT EXISTS INGESTA (
        id_ingesta TEXT PRIMARY KEY,
        fecha_consumo TEXT NOT NULL,
        lugar_consumo TEXT
);

CREATE TABLE IF NOT EXISTS INGESTA_PERSONA_EXPUESTA (
        id_ingesta_persona_expuesta TEXT PRIMARY KEY,
        id_ingesta TEXT NOT NULL,
        id_expuesto TEXT NOT NULL,
        es_sospechosa_para_expuesto INTEGER,
        FOREIGN KEY (id_ingesta) REFERENCES INGESTA(id_ingesta) ON DELETE CASCADE,
        FOREIGN KEY (id_expuesto) REFERENCES PERSONA_EXPUESTA(id_expuesto) ON DELETE CASCADE,
        UNIQUE (id_ingesta, id_expuesto)
);

CREATE TABLE IF NOT EXISTS ALIMENTO_CATALOGO (
        id_catalogo      TEXT PRIMARY KEY,
        nombre_canonico  TEXT NOT NULL UNIQUE,
        nombre_norm      TEXT NOT NULL UNIQUE,  -- nombre normalizado (minúsculas, sin acentos, espacios colapsados)
        categoria        TEXT
);

CREATE TABLE IF NOT EXISTS ALIMENTO_CATALOGO_ALIAS (
        alias            TEXT PRIMARY KEY,
        alias_norm       TEXT NOT NULL UNIQUE,  -- alias normalizado
        id_catalogo      TEXT NOT NULL,
        FOREIGN KEY (id_catalogo) REFERENCES ALIMENTO_CATALOGO(id_catalogo)
    );

-- GRUPOS
CREATE TABLE IF NOT EXISTS GRUPO_SINTOMA (
         id_grupo_sintomas TEXT PRIMARY KEY,
         descripcion_grupo TEXT NOT NULL
);

-- SÍNTOMAS
CREATE TABLE IF NOT EXISTS SINTOMA (
       id_sintoma         TEXT PRIMARY KEY,
       id_grupo_sintomas  TEXT NOT NULL,
       nombre_sintoma     TEXT NOT NULL,
       FOREIGN KEY (id_grupo_sintomas) REFERENCES GRUPO_SINTOMA(id_grupo_sintomas) ON DELETE RESTRICT
    );

-- CONJUNTO GENERAL DE SÍNTOMAS DE UN EXPUESTO
CREATE TABLE IF NOT EXISTS SINTOMAS_GENERALES_EXPUESTO (
           id_sintomas_generales  TEXT PRIMARY KEY,
           id_expuesto            TEXT NOT NULL,
           fecha_inicio_conjunto  TEXT,  -- 'YYYY-MM-DD HH:MM:SS'
           fecha_fin_conjunto     TEXT,  -- 'YYYY-MM-DD HH:MM:SS'
           observaciones_conjunto TEXT,
           FOREIGN KEY (id_expuesto) REFERENCES PERSONA_EXPUESTA(id_expuesto) ON DELETE CASCADE
    );

-- RELACIÓN N:M ENTRE CONJUNTO Y SÍNTOMA
CREATE TABLE IF NOT EXISTS EXPOSICION_SINTOMA (
        id_exposicion_sintoma   TEXT PRIMARY KEY,
        id_sintomas_generales   TEXT NOT NULL,
        id_sintoma              TEXT NOT NULL,
        FOREIGN KEY (id_sintomas_generales)
    REFERENCES SINTOMAS_GENERALES_EXPUESTO(id_sintomas_generales)
    ON DELETE CASCADE,
        FOREIGN KEY (id_sintoma)
    REFERENCES SINTOMA(id_sintoma) ON DELETE RESTRICT,
        UNIQUE (id_sintomas_generales, id_sintoma)  -- evita duplicados del mismo síntoma en el mismo conjunto
    );

-- Tabla de informes epidemiológicos por brote
CREATE TABLE IF NOT EXISTS INFORME (
       id_informe        TEXT PRIMARY KEY,
       id_brote          TEXT NOT NULL,
       contenido_informe TEXT,
       FOREIGN KEY (id_brote) REFERENCES BROTE(id_brote)
    );


CREATE TABLE IF NOT EXISTS CAMBIOS_PROCESADOS (
        id_cambio            TEXT PRIMARY KEY,
        timestamp_procesado  TEXT NOT NULL,
        instancia_origen     TEXT NOT NULL
);



CREATE INDEX IF NOT EXISTS idx_informe_brote ON INFORME(id_brote);
CREATE INDEX IF NOT EXISTS idx_sintoma_grupo ON SINTOMA(id_grupo_sintomas);
CREATE INDEX IF NOT EXISTS idx_exposicion_conjunto ON EXPOSICION_SINTOMA(id_sintomas_generales);
CREATE INDEX IF NOT EXISTS idx_generales_expuesto ON SINTOMAS_GENERALES_EXPUESTO(id_expuesto);



CREATE INDEX IF NOT EXISTS idx_pe_brote ON PERSONA_EXPUESTA(id_brote);

-- Rendimiento en consultas por brote
CREATE INDEX IF NOT EXISTS idx_expuesto_brote
    ON PERSONA_EXPUESTA(id_brote);

-- Unicidad de documento por brote (opcional pero recomendable)
-- Evita duplicados cuando hay (tipo_documento_expuesto, num_documento_expuesto)
CREATE UNIQUE INDEX IF NOT EXISTS ux_expuesto_doc
    ON PERSONA_EXPUESTA(
    id_brote,
    LOWER(tipo_documento_expuesto),
    UPPER(REPLACE(REPLACE(num_documento_expuesto,' ',''),'-',''))
    )
    WHERE num_documento_expuesto IS NOT NULL
    AND tipo_documento_expuesto IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_alimento_ingesta ON ALIMENTO(id_ingesta);
CREATE INDEX IF NOT EXISTS idx_ingesta_fecha ON INGESTA(fecha_consumo);
CREATE INDEX IF NOT EXISTS idx_ipe_ingesta ON INGESTA_PERSONA_EXPUESTA(id_ingesta);
CREATE INDEX IF NOT EXISTS idx_ipe_expuesto ON INGESTA_PERSONA_EXPUESTA(id_expuesto);
CREATE UNIQUE INDEX IF NOT EXISTS ux_ingesta_expuesto
    ON INGESTA_PERSONA_EXPUESTA(id_ingesta, id_expuesto);
CREATE UNIQUE INDEX IF NOT EXISTS ux_alimento_ingesta_nombre
    ON ALIMENTO(id_ingesta, nombre COLLATE NOCASE);

CREATE INDEX IF NOT EXISTS idx_alimento_catalogo ON ALIMENTO(id_catalogo);
CREATE INDEX IF NOT EXISTS idx_alias_norm        ON ALIMENTO_CATALOGO_ALIAS(alias_norm);
CREATE INDEX IF NOT EXISTS idx_catalogo_norm     ON ALIMENTO_CATALOGO(nombre_norm);
