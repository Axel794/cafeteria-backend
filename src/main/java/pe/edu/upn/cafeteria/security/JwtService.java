package pe.edu.upn.cafeteria.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.edu.upn.cafeteria.entity.Rol;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/** Genera y valida los tokens JWT (firmados con HMAC-SHA). El token lleva el usuario y su rol. */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMinutos;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.expiration-minutes}") long expiracionMinutos) {
        // La clave debe tener al menos 32 bytes (256 bits); en produccion se define con la variable JWT_SECRET.
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generarToken(String username, Rol rol) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("rol", rol.name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    /** Devuelve los datos del token si la firma es correcta y no expiro; si no, vacio. */
    public Optional<Claims> validar(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public long getExpiracionMinutos() {
        return expiracionMinutos;
    }
}
