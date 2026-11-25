#!/usr/bin/env bash
# Comprueba que los servicios responden en el entorno elegido.
# Uso:  ./verificar-disponibilidad.sh http://localhost:8081  barista  'ClaveDelBarista'
# (la URL base es la del frontend con nginx, o http://localhost:8080 si se prueba el backend solo)
BASE="${1:-http://localhost:8080}"; USUARIO="${2:-barista}"; CLAVE="${3:-Barista123*}"
ok=0; fallos=0
chequear() { # descripcion esperado obtenido
  if [ "$2" = "$3" ]; then printf '  OK     %-58s %s\n' "$1" "$3"; ok=$((ok+1));
  else printf '  FALLO  %-58s esperado %s, obtenido %s\n' "$1" "$2" "$3"; fallos=$((fallos+1)); fi; }
codigo() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

echo "Verificando $BASE  ($(date '+%Y-%m-%d %H:%M:%S'))"
chequear "GET  /api/v1/categorias (publico)"            200 "$(codigo "$BASE/api/v1/categorias")"
chequear "GET  /api/v1/productos (publico)"             200 "$(codigo "$BASE/api/v1/productos")"
chequear "GET  /api/v1/pedidos sin token"               401 "$(codigo "$BASE/api/v1/pedidos")"
chequear "GET  /api/v1/pedidos/historial sin token"     401 "$(codigo "$BASE/api/v1/pedidos/historial")"
chequear "POST /api/v1/auth/login con clave incorrecta" 401 "$(codigo -X POST -H 'Content-Type: application/json' -d "{\"username\":\"$USUARIO\",\"password\":\"incorrecta\"}" "$BASE/api/v1/auth/login")"
RESP=$(curl -s -X POST -H 'Content-Type: application/json' -d "{\"username\":\"$USUARIO\",\"password\":\"$CLAVE\"}" "$BASE/api/v1/auth/login")
TOKEN=$(printf '%s' "$RESP" | sed -n 's/.*"token"[ ]*:[ ]*"\([^"]*\)".*/\1/p')
if [ -n "$TOKEN" ]; then chequear "POST /api/v1/auth/login con credenciales validas" 200 200; else chequear "POST /api/v1/auth/login con credenciales validas" 200 "sin token"; fi
chequear "GET  /api/v1/pedidos/historial con token"     200 "$(codigo -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/pedidos/historial?page=0&size=5")"
chequear "GET  /api/v1/pedidos/historial size=51 (limite)" 400 "$(codigo -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/pedidos/historial?size=51")"
chequear "GET  /api/v1/no-existe con token"             404 "$(codigo -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/no-existe")"
echo "Resultado: $ok correctas, $fallos con fallo"
[ "$fallos" -eq 0 ]
