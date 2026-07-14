# inventory-core

Microservicio central de inventario B2B para SmartHome Shopper.

- **Stack:** Java 21, Spring Boot 3.2, MariaDB/MySQL
- **Puerto:** 8084
- **Contexto API:** `/api`

## Módulos incluidos

- `services/inventory-core` — servicio principal
- `shared/common-contracts` — contratos internos
- `shared/common-security` — JWT y seguridad compartida

## Build

```bash
./mvnw -pl services/inventory-core -am package -DskipTests
java -jar services/inventory-core/target/inventory-core-1.0.0.jar
```

## Variables de entorno

| Variable | Descripción |
|----------|-------------|
| `PORT` | Puerto HTTP |
| `MARIADB_*` | Base de datos |
| `JWT_SECRET` | Secret JWT |
| `APP_INTERNAL_TOKEN` | Token APIs internas |

## Docker

```bash
docker build -t inventory-core .
docker run -p 8084:8084 --env-file .env inventory-core
```
