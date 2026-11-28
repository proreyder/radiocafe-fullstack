-- PostgreSQL: migración aditiva, conservando los pedidos y sus propietarios actuales.
-- Respaldar radiocafe antes de aplicar. Hibernate update crea la tabla de tokens.
BEGIN;
ALTER TABLE pedidos ALTER COLUMN id_usuario DROP NOT NULL;
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS contacto_nombre varchar(100);
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS contacto_telefono varchar(30);
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS contacto_correo varchar(150);
COMMIT;
