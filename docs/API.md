# Documentación técnica de la API – Cafetería "Como dice el dicho"

Backend en Spring Boot 3.5 (Java 17), Spring Data JPA, Spring Security y JWT. URL base en desarrollo: `http://localhost:8080`. Todas las rutas empiezan por `/api/v1`. Las peticiones y respuestas usan JSON.

## 1. Entidades (tablas)

| Entidad | Campos principales | Relaciones |
| --- | --- | --- |
| Categoria | id, nombre (único), activa | 1 categoría tiene N productos |
| Producto | id, nombre, descripcion, precio, disponible, imagenUrl, alergenos | N productos pertenecen a 1 categoría; 1 producto tiene N opciones de personalización |
| OpcionPersonalizacion | id, nombre, precioAdicional, disponible | pertenece a 1 producto |
| Pedido | id, codigoTicket (único, ej. CDD-000012), fechaHora, estado, tipoPedido, numeroMesa, nombreCliente, telefonoCliente, total | 1 pedido tiene N detalles; lo atiende 1 usuario (opcional) |
| DetallePedido | id, cantidad, precioUnitario, subtotal, notas | pertenece a 1 pedido y a 1 producto |
| Usuario | id, username (único), passwordHash (BCrypt), rol | atiende pedidos |
| Configuracion | id, horaApertura, horaCierre, estadoLocal | — |

Valores permitidos: `estado` = PENDIENTE, EN_PREPARACION, LISTO, ENTREGADO · `tipoPedido` = LOCAL, PICKUP · `rol` = ADMINISTRADOR, BARISTA. El esquema SQL equivalente está en `docs/schema.sql`.

## 2. Endpoints

Acceso: **Público** (sin token), **Barista** (token de BARISTA o ADMINISTRADOR) o **Admin** (token de ADMINISTRADOR).

### Autenticación

| Método | Ruta | Acceso | Cuerpo | Respuestas |
| --- | --- | --- | --- | --- |
| POST | `/auth/login` | Público | `{"username","password"}` (ambos obligatorios) | 200 `{token, tipo:"Bearer", username, rol, expiraEnMinutos}` · 400 datos faltantes · 401 credenciales incorrectas |

### Categorías

| Método | Ruta | Acceso | Cuerpo / parámetros | Respuestas |
| --- | --- | --- | --- | --- |
| GET | `/categorias` | Público | — | 200 lista de categorías activas |
| GET | `/categorias/{id}` | Público | — | 200 · 404 |
| POST | `/categorias` | Admin | `{"nombre" (obligatorio, máx. 80), "activa"?}` | 201 · 400 · 401 · 403 · 409 nombre repetido |
| PUT | `/categorias/{id}` | Admin | igual que POST | 200 · 400 · 401 · 403 · 404 · 409 nombre usado por otra categoría |
| DELETE | `/categorias/{id}` | Admin | — | 204 · 401 · 403 · 404 · 409 si tiene productos |

### Productos

| Método | Ruta | Acceso | Cuerpo / parámetros | Respuestas |
| --- | --- | --- | --- | --- |
| GET | `/productos` | Público | `categoriaId?`, `soloDisponibles?` (true/false) | 200 lista ordenada por nombre · 400 parámetro inválido |
| GET | `/productos/{id}` | Público | — | 200 · 404 |
| POST | `/productos` | Admin | `{"nombre" (obligatorio, máx. 120), "descripcion"?, "precio" (> 0), "categoriaId", "imagenUrl"?, "alergenos"?, "disponible"?}` | 201 · 400 · 401 · 403 · 404 categoría inexistente |
| PUT | `/productos/{id}` | Admin | igual que POST (si `disponible` no se envía, no cambia) | 200 · 400 · 401 · 403 · 404 |
| DELETE | `/productos/{id}` | Admin | — | 204 · 401 · 403 · 404 · 409 si ya tiene pedidos (usar `disponible=false`) |

### Pedidos

| Método | Ruta | Acceso | Cuerpo / parámetros | Respuestas |
| --- | --- | --- | --- | --- |
| POST | `/pedidos` | Público | `{"tipoPedido", "numeroMesa"?, "nombreCliente"?, "telefonoCliente"?, "detalles":[{"productoId","cantidad" (1 a 20),"notas"?}]}` | 201 con ticket, estado PENDIENTE y total · 400 · 404 producto inexistente · 422 regla de negocio |
| GET | `/pedidos/{id}/tracker` | Público | — | 200 estado y mensaje para el cliente · 404 |
| GET | `/pedidos` | Barista | `estado?` | 200 lista (más antiguos primero) · 400 · 401 · 403 |
| GET | `/pedidos/{id}` | Barista | — | 200 con detalle completo · 401 · 403 · 404 |
| PUT | `/pedidos/{id}/estado` | Barista | `{"estado"}` | 200 · 400 · 401 · 403 · 404 · 422 transición no permitida |

Reglas de negocio de los pedidos (422): LOCAL exige `numeroMesa` mayor que 0; PICKUP exige nombre y teléfono; no se pueden pedir productos no disponibles; el estado solo avanza un paso (PENDIENTE → EN_PREPARACION → LISTO → ENTREGADO), sin saltos ni retrocesos, y un pedido ENTREGADO no cambia más. Los subtotales y el total los calcula siempre el servidor con el precio vigente del producto.

## 3. Formato de errores

Todos los errores, incluidos 401 y 403, devuelven el mismo JSON:

```json
{
  "timestamp": "2026-11-04T17:10:00.123",
  "status": 422,
  "error": "Unprocessable Entity",
  "mensaje": "No se puede pasar de PENDIENTE a LISTO; el siguiente estado permitido es EN_PREPARACION",
  "detalles": []
}
```

| Código | Cuándo |
| --- | --- |
| 400 | Validación de datos (`detalles` lista cada campo), JSON mal formado, valor de enum o parámetro inválido |
| 401 | Sin token, token inválido, expirado o firmado con otra clave; credenciales de login incorrectas |
| 403 | Token válido pero el rol no tiene permiso para esa acción |
| 404 | El recurso (o la ruta) no existe |
| 409 | Conflicto: nombre duplicado o recurso en uso (categoría con productos, producto con pedidos) |
| 422 | Regla de negocio incumplida |
| 500 | Error inesperado (se registra en el log; el cliente solo ve un mensaje genérico) |

## 4. Seguridad implementada

- **Spring Security sin sesiones (stateless).** CSRF desactivado porque el token viaja en una cabecera y no en cookies.
- **Login con JWT.** `POST /auth/login` valida usuario y contraseña y devuelve un token firmado con HMAC-SHA que contiene el usuario y su rol y expira a los 120 minutos (`app.jwt.expiration-minutes`). Se envía en cada petición como `Authorization: Bearer <token>`.
- **Contraseñas cifradas con BCrypt**; nunca se devuelven en las respuestas. El mensaje de login fallido es el mismo si el usuario no existe o la contraseña es incorrecta.
- **Control de acceso por rol:** el administrador gestiona el catálogo (POST, PUT y DELETE de categorías y productos); el barista y el administrador atienden pedidos (listado, detalle y cambio de estado); la carta, el registro de pedidos y el tracker son públicos.
- **CORS:** solo se aceptan los orígenes de `app.cors.allowed-origins` (por defecto `http://localhost:4200` y `http://127.0.0.1:4200`, el servidor de Angular), con los métodos GET, POST, PUT, DELETE y OPTIONS y las cabeceras Authorization, Content-Type y Accept.
- **Configuración sensible por variables de entorno:** `JWT_SECRET` (mínimo 32 caracteres), `SEED_ADMIN_PASSWORD`, `SEED_BARISTA_PASSWORD`, `DB_USER` y `DB_PASSWORD`.

Usuarios de prueba que se crean al primer arranque (solo desarrollo): `admin` / `Admin123*` y `barista` / `Barista123*`.

### Uso desde Angular

1. Llamar a `POST /api/v1/auth/login` y guardar `token`.
2. Añadir `Authorization: Bearer <token>` en las peticiones protegidas (por ejemplo con un `HttpInterceptor`).
3. Si la API responde 401, redirigir al login.

## 5. Limitaciones conocidas

- La consola H2 (`/h2-console`) está abierta para desarrollo; debe desactivarse (`spring.h2.console.enabled=false`) fuera de él.
- No hay renovación de token (refresh token) ni cierre de sesión en el servidor: el token es válido hasta que expira.
- No hay límite de intentos de login ni gestión de usuarios por la API; los usuarios se crean al iniciar.
- La disponibilidad es un valor sí/no, sin control de stock por cantidad.
- No hay paginación en los listados (adecuado para el tamaño actual de la carta).
- Los endpoints de opciones de personalización (RF02) y el reporte del dashboard (HU09) quedan para el siguiente sprint.
- Los datos de arranque (precios, horario) son de ejemplo.
