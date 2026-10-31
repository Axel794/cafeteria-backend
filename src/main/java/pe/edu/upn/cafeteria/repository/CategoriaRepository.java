package pe.edu.upn.cafeteria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upn.cafeteria.entity.Categoria;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByActivaTrueOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);
}
