package pe.edu.upn.cafeteria.exception;

import org.springframework.http.HttpStatus;

/** Se lanza cuando una peticion valida en formato incumple una regla del negocio. */
public class ReglaNegocioException extends RuntimeException {

    private final HttpStatus status;

    public ReglaNegocioException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
