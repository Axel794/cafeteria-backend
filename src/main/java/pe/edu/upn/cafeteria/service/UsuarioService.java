package pe.edu.upn.cafeteria.service;

import pe.edu.upn.cafeteria.entity.Rol;
import pe.edu.upn.cafeteria.entity.Usuario;
import pe.edu.upn.cafeteria.repository.RolRepository;
import pe.edu.upn.cafeteria.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public Usuario guardarUsuario(Usuario usuario) {
        if (usuario.getRol() != null && usuario.getRol().getId() != null) {
            // Buscamos si el rol ya existe en la base de datos
            Rol rolDb = rolRepository.findById(usuario.getRol().getId()).orElse(null);
            if (rolDb != null) {
                usuario.setRol(rolDb);
            } else {
                // Si no existe, creamos un nuevo rol dejando que la BD genere el ID automáticamente
                Rol nuevoRol = new Rol();
                nuevoRol.setNombre("ADMIN");
                Rol rolGuardado = rolRepository.save(nuevoRol);
                usuario.setRol(rolGuardado);
            }
        }
        return usuarioRepository.save(usuario);
    }
}