package pe.edu.upn.cafeteria.dto;

/** Respuesta del login: el token se envia luego en la cabecera "Authorization: Bearer <token>". */
public record LoginResponse(
        String token,
        String tipo,
        String username,
        String rol,
        long expiraEnMinutos) {
}
