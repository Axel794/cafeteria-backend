package pe.edu.upn.cafeteria.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import pe.edu.upn.cafeteria.dto.ErrorResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/** Responde 401 (sin token o token invalido) y 403 (sin permisos) con el mismo JSON de error del resto de la API. */
@Component
public class ApiSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ApiSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        escribir(response, HttpStatus.UNAUTHORIZED,
                "Se requiere un token valido. Inicie sesion en POST /api/v1/auth/login");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escribir(response, HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta accion");
    }

    private void escribir(HttpServletResponse response, HttpStatus status, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErrorResponse cuerpo = new ErrorResponse(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensaje, List.of());
        objectMapper.writeValue(response.getWriter(), cuerpo);
    }
}
