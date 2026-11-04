package pe.edu.upn.cafeteria.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import pe.edu.upn.cafeteria.dto.LoginRequest;
import pe.edu.upn.cafeteria.dto.LoginResponse;
import pe.edu.upn.cafeteria.entity.Usuario;
import pe.edu.upn.cafeteria.exception.ReglaNegocioException;
import pe.edu.upn.cafeteria.repository.UsuarioRepository;
import pe.edu.upn.cafeteria.security.JwtService;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager,
                       UsuarioRepository usuarioRepository,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    /** HU07: valida usuario y contrasena y entrega un token JWT. El mensaje de error no revela cual de los dos fallo. */
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            throw new ReglaNegocioException(HttpStatus.UNAUTHORIZED, "Usuario o contrasena incorrectos");
        }

        Usuario usuario = usuarioRepository.findByUsername(request.username())
                .orElseThrow(() -> new ReglaNegocioException(HttpStatus.UNAUTHORIZED, "Usuario o contrasena incorrectos"));

        String token = jwtService.generarToken(usuario.getUsername(), usuario.getRol());
        return new LoginResponse(token, "Bearer", usuario.getUsername(), usuario.getRol().name(),
                jwtService.getExpiracionMinutos());
    }
}
