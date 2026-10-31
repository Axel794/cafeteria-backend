-- Esquema de referencia (PostgreSQL) equivalente al que genera Hibernate con ddl-auto=update.
-- Las tablas se crean solas al arrancar la aplicacion; este archivo sirve de documentacion
-- y para crear la base manualmente si se prefiere.

CREATE TABLE categoria (
    id      BIGSERIAL PRIMARY KEY,
    nombre  VARCHAR(80) NOT NULL UNIQUE,
    activa  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE producto (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(120) NOT NULL,
    descripcion  VARCHAR(500),
    precio       NUMERIC(10,2) NOT NULL,
    disponible   BOOLEAN NOT NULL DEFAULT TRUE,
    imagen_url   VARCHAR(255),
    alergenos    VARCHAR(255),
    categoria_id BIGINT NOT NULL REFERENCES categoria (id)
);

CREATE TABLE opcion_personalizacion (
    id               BIGSERIAL PRIMARY KEY,
    nombre           VARCHAR(80) NOT NULL,
    precio_adicional NUMERIC(10,2) NOT NULL DEFAULT 0,
    disponible       BOOLEAN NOT NULL DEFAULT TRUE,
    producto_id      BIGINT NOT NULL REFERENCES producto (id)
);

CREATE TABLE usuario (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol           VARCHAR(20) NOT NULL          -- ADMINISTRADOR | BARISTA
);

CREATE TABLE configuracion (
    id            BIGSERIAL PRIMARY KEY,
    hora_apertura TIME NOT NULL,
    hora_cierre   TIME NOT NULL,
    estado_local  VARCHAR(20) NOT NULL          -- ABIERTO | CERRADO
);

CREATE TABLE pedido (
    id               BIGSERIAL PRIMARY KEY,
    codigo_ticket    VARCHAR(20) UNIQUE,        -- ej. CDD-000012
    fecha_hora       TIMESTAMP NOT NULL,
    estado           VARCHAR(20) NOT NULL,      -- PENDIENTE | EN_PREPARACION | LISTO | ENTREGADO
    tipo_pedido      VARCHAR(20) NOT NULL,      -- LOCAL | PICKUP
    numero_mesa      INTEGER,
    nombre_cliente   VARCHAR(100),
    telefono_cliente VARCHAR(20),
    total            NUMERIC(10,2) NOT NULL,
    usuario_id       BIGINT REFERENCES usuario (id)
);

CREATE TABLE detalle_pedido (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT NOT NULL REFERENCES pedido (id),
    producto_id     BIGINT NOT NULL REFERENCES producto (id),
    cantidad        INTEGER NOT NULL,
    precio_unitario NUMERIC(10,2) NOT NULL,
    subtotal        NUMERIC(10,2) NOT NULL,
    notas           VARCHAR(255)
);
