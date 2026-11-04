# Cafetería "Como dice el dicho" – Backend (Prácticas de campo 2 y 3)

Backend del sistema de **menú digital y pedidos** hecho con Spring Boot, Spring Data JPA, Spring Security (JWT) y una base de datos relacional. La práctica 2 cubrió el Sprint 1 (HU01, HU03 y la parte de servidor de HU02/HU06); la práctica 3 consolida la API con CRUD completo, validaciones, CORS para Angular y seguridad (HU04, HU05, HU07 y HU08 en su parte de servidor).

## Requisitos

- JDK 17 o superior
- Maven 3.9+ (o el Maven que trae IntelliJ IDEA)
- Opcional: Docker, solo para usar PostgreSQL
- Postman, para importar `docs/cafeteria-postman-collection.json`

## Cómo ejecutarlo

### Opción A – H2 (no hay que instalar nada)

```bash
mvn spring-boot:run
```

- API: http://localhost:8080/api/v1/productos
- Consola de la base de datos (evidencia de conexión): http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/cafeteria`, usuario `sa`, contraseña vacía
- La primera vez se cargan datos de **prueba** (4 categorías y 8 productos de ejemplo).

### Opción B – PostgreSQL

```bash
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

Credenciales por defecto: usuario `postgres`, contraseña `postgres`, base `cafeteria`. Se pueden cambiar con las variables `DB_USER` y `DB_PASSWORD`. El esquema equivalente está en `docs/schema.sql`.

## Pruebas

```bash
mvn test
```

Ejecuta 66 pruebas de integración (MockMvc + H2 en memoria): CRUD completo, validaciones, errores 400/404/409/422, login, 401 sin token o con token inválido/expirado, 403 por rol y CORS para Angular.

Para probar con Postman: importar `docs/cafeteria-postman-collection.json` y ejecutarla en orden con el **Runner**, con la aplicación levantada. Cada petición trae sus verificaciones (`pm.test`); las dos primeras guardan los tokens de administrador y barista.

## Seguridad

La API usa Spring Security con JWT. Se obtiene un token con `POST /api/v1/auth/login` y se envía como `Authorization: Bearer <token>`.

| Usuario de prueba | Contraseña | Rol |
| --- | --- | --- |
| admin | Admin123* | ADMINISTRADOR |
| barista | Barista123* | BARISTA |

Son usuarios de **desarrollo** que se crean al primer arranque. Fuera de desarrollo hay que definir `JWT_SECRET`, `SEED_ADMIN_PASSWORD` y `SEED_BARISTA_PASSWORD`. El detalle de rutas, roles, errores y medidas de seguridad está en [`docs/API.md`](docs/API.md).

## Endpoints implementados

Acceso: público (P), barista o administrador (B), solo administrador (A).

| Método | Ruta | Acceso | Descripción |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/login` | P | Devuelve el token JWT |
| GET | `/api/v1/categorias` | P | Categorías activas |
| GET | `/api/v1/categorias/{id}` | P | Detalle de una categoría |
| POST | `/api/v1/categorias` | A | Crear categoría (409 si el nombre existe) |
| PUT | `/api/v1/categorias/{id}` | A | Actualizar categoría |
| DELETE | `/api/v1/categorias/{id}` | A | Eliminar (409 si tiene productos) |
| GET | `/api/v1/productos` | P | Catálogo, con filtros `categoriaId` y `soloDisponibles` |
| GET | `/api/v1/productos/{id}` | P | Detalle de un producto |
| POST | `/api/v1/productos` | A | Crear producto |
| PUT | `/api/v1/productos/{id}` | A | Actualizar precio, datos y disponibilidad |
| DELETE | `/api/v1/productos/{id}` | A | Eliminar (409 si ya tiene pedidos) |
| POST | `/api/v1/pedidos` | P | Registrar pedido (LOCAL o PICKUP), estado inicial PENDIENTE |
| GET | `/api/v1/pedidos/{id}/tracker` | P | Estado del pedido para el cliente |
| GET | `/api/v1/pedidos` | B | Comandas, filtro `estado`, las más antiguas primero |
| GET | `/api/v1/pedidos/{id}` | B | Detalle de un pedido |
| PUT | `/api/v1/pedidos/{id}/estado` | B | Avanzar el estado (sin saltos ni retrocesos) |

Ejemplo de pedido:

```json
POST /api/v1/pedidos
{
  "tipoPedido": "LOCAL",
  "numeroMesa": 5,
  "detalles": [
    { "productoId": 1, "cantidad": 2, "notas": "sin azúcar" }
  ]
}
```

Los errores devuelven un JSON con `status`, `mensaje` y `detalles`: 400 (datos inválidos), 401 (sin token o token inválido), 403 (rol sin permiso), 404 (no existe), 409 (duplicado o en uso) y 422 (regla de negocio).

## Estructura

```
src/main/java/pe/edu/upn/cafeteria
├── controller   Endpoints REST (reciben y responden JSON)
├── service      Reglas de negocio (modalidad, disponibilidad, total, ticket)
├── repository   Interfaces Spring Data JPA
├── entity       Entidades JPA y enums
├── dto          Objetos de entrada y salida de la API
├── exception    Excepciones y manejador global de errores
├── security     JWT, filtro de autenticación, roles y CORS
└── config       Carga de datos de prueba
```

## Pendiente (siguientes sprints)

- Endpoints de personalización de productos (la entidad ya existe).
- Reporte de ventas del dashboard (HU09).
- Frontend en Angular que consuma esta API.
- Control de stock por cantidad, renovación de token y paginación.
