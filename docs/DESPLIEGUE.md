# Guía de despliegue – Cafetería "Como dice el dicho"

## 1. Arquitectura del despliegue

```
Navegador ──► nginx (frontend Angular, puerto 8081) ──/api/──► Spring Boot (backend, interno 8080) ──► PostgreSQL 16 (interno 5432)
```

- Tres contenedores: `frontend` (nginx con los archivos de Angular), `backend` (Spring Boot, perfil `prod`) y `db` (PostgreSQL con volumen persistente).
- Solo se publica el puerto **8081** (nginx). El backend y la base de datos quedan en la red interna de Docker.
- Angular se compila con `environment.prod.ts` (`apiUrl: '/api/v1'`, ruta relativa); nginx reenvía `/api/` al backend, por eso no hace falta CORS.

## 2. Requisitos

- Docker Desktop (o Docker Engine con el plugin `compose`).
- Las carpetas `cafeteria-backend` y `cafeteria-frontend` una junto a la otra (así lo espera `deploy/docker-compose.full.yml`).

## 3. Pasos (Opción A: Docker Compose, el entorno elegido)

1. Abrir una terminal en `cafeteria-backend/deploy`.
2. Crear el archivo de variables: `copy .env.example .env` (Windows) o `cp .env.example .env` (Linux/Mac). Editar `.env` y cambiar todas las contraseñas; `JWT_SECRET` debe tener **32 caracteres o más**. `.env` no se sube a GitHub (está en `.gitignore`).
3. Levantar todo: `docker compose -f docker-compose.full.yml --env-file .env up -d --build`. La primera vez descarga imágenes y compila (varios minutos).
4. Ver el estado: `docker compose -f docker-compose.full.yml ps` (los tres servicios en `running`/`healthy`) y los registros con `docker compose -f docker-compose.full.yml logs backend`.
5. Abrir `http://localhost:8081` (carta) e iniciar sesión en `/login` con `barista` o `admin` y las claves puestas en `.env`.
6. Verificar disponibilidad con el script (ver sección 5).
7. Apagar: `docker compose -f docker-compose.full.yml down` (conserva los datos); con `-v` también borra la base de datos.

## 4. Variables de entorno (perfil `prod`)

| Variable | Uso | Observación |
| --- | --- | --- |
| `DB_URL` | URL JDBC de PostgreSQL | En compose: `jdbc:postgresql://db:5432/cafeteria` |
| `DB_USER`, `DB_PASSWORD` | Credenciales de la base | Obligatorias |
| `JWT_SECRET` | Clave de firma de los tokens | Obligatoria, mínimo 32 caracteres, aleatoria |
| `SEED_ADMIN_PASSWORD`, `SEED_BARISTA_PASSWORD` | Claves de los usuarios iniciales | Obligatorias |
| `SWAGGER_ENABLED` | Activa Swagger UI | `false` por defecto |
| `CORS_ORIGINS` | Orígenes permitidos | Solo si el frontend se sirve desde otro dominio |

Si falta una variable obligatoria, el backend no arranca (error en el registro): es intencional.

## 5. Comprobar la disponibilidad

```
bash deploy/verificar-disponibilidad.sh http://localhost:8081 barista 'ClaveDelBarista'
```

(En Windows se puede ejecutar con Git Bash.) Comprueba 9 puntos: carta y productos públicos (200), pedidos sin token (401), login incorrecto (401) y correcto (200), historial con token (200), límite de tamaño de página (400) y ruta inexistente (404). Termina con `Resultado: N correctas, M con fallo`; guardar esa salida como evidencia.

Comprobaciones manuales complementarias: crear un pedido desde la carta, seguirlo en `/seguimiento`, atenderlo en `/panel` y verlo en `/historial`.

## 6. Opción B: publicación en la nube (no ejecutada en esta práctica)

El mismo diseño se traslada a un proveedor que acepte contenedores: backend como servicio Docker con las variables de la sección 4, base PostgreSQL administrada (`DB_URL` con la URL del proveedor) y el frontend como sitio estático (o el contenedor nginx) con la regla `/api/*` hacia el backend. Antes de publicar: HTTPS obligatorio, claves nuevas, `SWAGGER_ENABLED=false` y copias de seguridad de la base.

## 7. Limitaciones del despliegue

- No se publicó en un servidor público; la evidencia es el despliegue local con Docker Compose.
- Sin HTTPS en el entorno local (se resolvería con un proxy inverso y certificado).
- `ddl-auto=update` crea las tablas automáticamente; con datos reales convendría migraciones (Flyway).
- Los usuarios iniciales se crean una sola vez al primer arranque; no hay gestión de usuarios por la API.
- Sin monitoreo ni copias de seguridad automáticas.
