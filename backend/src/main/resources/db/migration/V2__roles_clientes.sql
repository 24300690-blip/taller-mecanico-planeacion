ALTER TABLE users DROP CHECK chk_users_role;
UPDATE users SET role_name = 'ADMINISTRADOR' WHERE role_name = 'ADMIN';
ALTER TABLE users MODIFY role_name VARCHAR(32) NOT NULL;
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role_name IN ('ADMINISTRADOR','GERENTE','RECEPCIONISTA','MECANICO','AYUDANTE'));

CREATE TABLE role_catalog (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(32) NOT NULL UNIQUE,
  description VARCHAR(160) NOT NULL
);
INSERT INTO role_catalog(name, description) VALUES
 ('ADMINISTRADOR','Administración total del sistema'), ('GERENTE','Gestión operativa del taller'),
 ('RECEPCIONISTA','Atención y registro de clientes'), ('MECANICO','Ejecución de reparaciones'), ('AYUDANTE','Apoyo a mecánicos');

CREATE TABLE empresa (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 nombre VARCHAR(160) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE taller (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 empresa_id BIGINT NOT NULL,
 nombre VARCHAR(160) NOT NULL,
 calle VARCHAR(180) NOT NULL,
 colonia VARCHAR(120) NOT NULL,
 municipio VARCHAR(120) NOT NULL,
 estado VARCHAR(120) NOT NULL,
 codigo_postal CHAR(5) NOT NULL,
 CONSTRAINT fk_taller_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);
CREATE TABLE cliente (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 nombres VARCHAR(120) NOT NULL,
 apellido_paterno VARCHAR(80) NOT NULL,
 apellido_materno VARCHAR(80) NULL,
 fecha_nacimiento DATE NOT NULL,
 telefono_personal CHAR(10) NOT NULL,
 telefono_trabajo VARCHAR(20) NULL,
 correo_personal VARCHAR(254) NOT NULL,
 correo_trabajo VARCHAR(254) NULL,
 foto_path VARCHAR(700) NULL,
 calle VARCHAR(180) NOT NULL,
 colonia VARCHAR(120) NOT NULL,
 municipio VARCHAR(120) NOT NULL,
 estado VARCHAR(120) NOT NULL,
 codigo_postal CHAR(5) NOT NULL,
 nombre_normalizado VARCHAR(300) NOT NULL,
 empresa_id BIGINT NULL,
 created_by BIGINT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_cliente_correo UNIQUE(correo_personal),
 CONSTRAINT uq_cliente_telefono UNIQUE(telefono_personal),
 CONSTRAINT uq_cliente_nombre_fecha UNIQUE(nombre_normalizado, fecha_nacimiento),
 CONSTRAINT fk_cliente_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
 CONSTRAINT fk_cliente_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);
CREATE TABLE cliente_taller (
 cliente_id BIGINT NOT NULL,
 taller_id BIGINT NOT NULL,
 PRIMARY KEY(cliente_id, taller_id),
 CONSTRAINT fk_cliente_taller_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
 CONSTRAINT fk_cliente_taller_taller FOREIGN KEY (taller_id) REFERENCES taller(id)
);
