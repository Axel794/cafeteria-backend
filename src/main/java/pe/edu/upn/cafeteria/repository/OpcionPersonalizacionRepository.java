package pe.edu.upn.cafeteria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upn.cafeteria.entity.OpcionPersonalizacion;

import java.util.List;

public interface OpcionPersonalizacionRepository extends JpaRepository<OpcionPersonalizacion, Long> {

    List<OpcionPersonalizacion> findByProductoIdAndDisponibleTrue(Long productoId);
}
