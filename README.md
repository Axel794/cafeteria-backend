# Cafetería "Como dice el dicho" – Backend (Práctica de campo 2)

Backend inicial del sistema de **menú digital y pedidos** hecho con Spring Boot, Spring Data JPA y una base de datos relacional. Corresponde al **Sprint 1** del backlog (HU01, HU03 y la parte de servidor de HU02/HU06).

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

Ejecuta 18 pruebas de integración (MockMvc + H2 en memoria) que cubren los casos correctos y los errores de cada endpoint.

Para probar con Postman: importar `docs/cafeteria-postman-collection.json` y ejecutarla con el **Runner**, con la aplicación levantada. Cada petición trae sus verificaciones (`pm.test`).

## Endpoints implementados

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/v1/categorias` | Categorías activas |
| POST | `/api/v1/categorias` | Crear categoría (409 si el nombre existe) |
| GET | `/api/v1/productos` | Catálogo, con filtros `categoriaId` y `soloDisponibles` |
| GET | `/api/v1/productos/{id}` | Detalle de un producto |
| POST | `/api/v1/productos` | Crear producto |
| POST | `/api/v1/pedidos` | Registrar pedido (LOCAL o PICKUP), estado inicial PENDIENTE |
| GET | `/api/v1/pedidos/{id}` | Detalle de un pedido |
| GET | `/api/v1/pedidos/{id}/tracker` | Estado del pedido para el cliente |

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

Los errores devuelven un JSON con `status`, `mensaje` y `detalles`: 400 (datos inválidos), 404 (no existe), 409 (duplicado) y 422 (regla de negocio, por ejemplo producto agotado).

## Estructura

```
src/main/java/pe/edu/upn/cafeteria
├── controller   Endpoints REST (reciben y responden JSON)
├── service      Reglas de negocio (modalidad, disponibilidad, total, ticket)
├── repository   Interfaces Spring Data JPA
├── entity       Entidades JPA y enums
├── dto          Objetos de entrada y salida de la API
├── exception    Excepciones y manejador global de errores
└── config       CORS y carga de datos de prueba
```

## Pendiente (siguientes sprints)

- Login y JWT (HU07): hoy `POST /categorias` y `POST /productos` son públicos.
- Dashboard KDS y cambio de estado del pedido (HU04, HU05).
- Endpoints de personalización de productos (la entidad ya existe).
- Edición y baja de productos (HU08) y reporte del dashboard (HU09).
