CREATE DATABASE marketplace
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE marketplace;

-- Cuentas de vendedores y administradores.
-- Guarda siempre la contraseña como hash, nunca como texto plano.
CREATE TABLE usuarios (
    id              INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    correo          VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    rol             ENUM('vendedor', 'administrador') NOT NULL DEFAULT 'vendedor',
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categorias (
    id              INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE tiendas (
    id              INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id      INT UNSIGNED NOT NULL UNIQUE,
    nombre          VARCHAR(120) NOT NULL,
    descripcion     TEXT,
    telefono        VARCHAR(30),
    whatsapp        VARCHAR(30),
    correo_contacto VARCHAR(150),
    estado          ENUM('pendiente', 'aprobada', 'rechazada', 'suspendida')
                    NOT NULL DEFAULT 'pendiente',
    creada_en       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tiendas_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE productos (
    id              INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tienda_id       INT UNSIGNED NOT NULL,
    categoria_id    INT UNSIGNED,
    nombre          VARCHAR(150) NOT NULL,
    descripcion     TEXT,
    precio          DECIMAL(10, 2) NOT NULL,
    disponibilidad  ENUM('disponible', 'no_disponible')
                    NOT NULL DEFAULT 'disponible',
    estado          ENUM('pendiente', 'publicado', 'rechazado', 'retirado')
                    NOT NULL DEFAULT 'pendiente',
    creado_en       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_productos_tienda
        FOREIGN KEY (tienda_id) REFERENCES tiendas(id),
    CONSTRAINT fk_productos_categoria
        FOREIGN KEY (categoria_id) REFERENCES categorias(id),

    INDEX idx_productos_catalogo (estado, categoria_id, precio),
    INDEX idx_productos_tienda (tienda_id, estado)
);

-- Un producto puede tener varias fotos.
CREATE TABLE fotos_productos (
    id              INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    producto_id     INT UNSIGNED NOT NULL,
    url             VARCHAR(500) NOT NULL,
    principal       BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_fotos_producto
        FOREIGN KEY (producto_id) REFERENCES productos(id)
        ON DELETE CASCADE
);