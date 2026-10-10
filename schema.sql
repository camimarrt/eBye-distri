-- Base de datos de eBye.
--
-- 1. Crear la base (una sola vez, desde psql o pgAdmin):
--      CREATE DATABASE ebye;
-- 2. Conectarse a la base ebye y correr este archivo:
--      psql -U postgres -d ebye -f schema.sql

DROP TABLE IF EXISTS producto;

CREATE TABLE producto (
    id      INTEGER PRIMARY KEY,
    nombre  VARCHAR(100) NOT NULL,
    stock   INTEGER NOT NULL,
    precio  BIGINT NOT NULL
);

INSERT INTO producto (id, nombre, stock, precio) VALUES (305, 'Auriculares Bluetooth', 15, 85000);
INSERT INTO producto (id, nombre, stock, precio) VALUES (306, 'Mouse inalambrico', 42, 45000);
INSERT INTO producto (id, nombre, stock, precio) VALUES (307, 'Teclado mecanico', 0, 220000);
