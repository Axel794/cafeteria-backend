package pe.edu.upn.cafeteria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upn.cafeteria.entity.Producto;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findAllByOrderByNombreAsc();

    List<Producto> findByCategoriaIdOrderByNombreAsc(Long categoriaId);
}
