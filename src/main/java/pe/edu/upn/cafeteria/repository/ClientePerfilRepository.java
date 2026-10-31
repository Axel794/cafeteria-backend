package pe.edu.upn.cafeteria.repository;

import pe.edu.upn.cafeteria.entity.ClientePerfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientePerfilRepository extends JpaRepository<ClientePerfil, Long> {
}