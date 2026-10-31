package pe.edu.upn.cafeteria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upn.cafeteria.entity.Configuracion;

public interface ConfiguracionRepository extends JpaRepository<Configuracion, Long> {
}
