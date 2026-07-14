-- RBAC para MariaDB/MySQL — equivalente a V4 PostgreSQL

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS modules (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    `key` VARCHAR(64) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS role_modules (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    module_id BIGINT NOT NULL,
    can_create TINYINT(1) NOT NULL DEFAULT 0,
    can_read TINYINT(1) NOT NULL DEFAULT 0,
    can_update TINYINT(1) NOT NULL DEFAULT 0,
    can_delete TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT uk_role_module_pair UNIQUE (role_id, module_id),
    CONSTRAINT fk_rm_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_rm_module FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_role_modules_role ON role_modules(role_id);
CREATE INDEX idx_role_modules_module ON role_modules(module_id);

INSERT IGNORE INTO roles (id, name) VALUES
    (1, 'PLATFORM_OWNER'),
    (2, 'MANAGER'),
    (3, 'MEMBER'),
    (4, 'VIEWER');

INSERT IGNORE INTO modules (id, name, `key`) VALUES
    (1, 'Inventario', 'INVENTORY'),
    (2, 'Compras', 'PURCHASES'),
    (3, 'Informes', 'REPORTS'),
    (4, 'Usuarios', 'USERS');

INSERT INTO role_modules (role_id, module_id, can_create, can_read, can_update, can_delete) VALUES
    (1, 1, 1, 1, 1, 1),
    (1, 2, 1, 1, 1, 1),
    (1, 3, 1, 1, 1, 1),
    (1, 4, 1, 1, 1, 1),
    (2, 1, 1, 1, 1, 1),
    (2, 2, 1, 1, 1, 1),
    (2, 3, 1, 1, 1, 1),
    (2, 4, 1, 1, 1, 0),
    (3, 1, 1, 1, 1, 1),
    (3, 2, 0, 1, 0, 0),
    (3, 3, 0, 1, 0, 0),
    (3, 4, 0, 1, 0, 0),
    (4, 1, 0, 1, 0, 0),
    (4, 2, 0, 1, 0, 0),
    (4, 3, 0, 1, 0, 0),
    (4, 4, 0, 1, 0, 0)
ON DUPLICATE KEY UPDATE
    can_create = VALUES(can_create),
    can_read = VALUES(can_read),
    can_update = VALUES(can_update),
    can_delete = VALUES(can_delete);

ALTER TABLE users ADD COLUMN role_id BIGINT NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id);

UPDATE users SET role_id = 3 WHERE role_id IS NULL;

CREATE INDEX idx_users_role ON users(role_id);
