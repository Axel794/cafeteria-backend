package pe.edu.upn.cafeteria.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee la cabecera "Authorization: Bearer <token>" y, si el token es valido, autentica la peticion
 * con el rol que trae. Si no hay token o es invalido no autentica: las rutas protegidas responderan 401.
 * No es un @Component a proposito: se registra solo dentro de la cadena de Spring Security.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            String token = cabecera.substring(PREFIJO.length()).trim();
            jwtService.validar(token).ifPresent(claims -> {
                String rol = claims.get("rol", String.class);
                if (rol != null) {
                    UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(
                            claims.getSubject(), null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
                    autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                }
            });
        }

        filterChain.doFilter(request, response);
    }
}
