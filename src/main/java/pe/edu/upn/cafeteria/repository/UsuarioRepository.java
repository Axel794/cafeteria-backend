package pe.edu.upn.cafeteria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upn.cafeteria.entity.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);
}
