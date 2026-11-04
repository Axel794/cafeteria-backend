package pe.edu.upn.cafeteria.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.cafeteria.dto.CategoriaRequest;
import pe.edu.upn.cafeteria.dto.CategoriaResponse;
import pe.edu.upn.cafeteria.entity.Categoria;
import pe.edu.upn.cafeteria.exception.RecursoNoEncontradoException;
import pe.edu.upn.cafeteria.exception.ReglaNegocioException;
import pe.edu.upn.cafeteria.repository.CategoriaRepository;
import pe.edu.upn.cafeteria.repository.ProductoRepository;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarActivas() {
        return categoriaRepository.findByActivaTrueOrderByNombreAsc().stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        String nombre = request.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "Ya existe una categoria con el nombre '" + nombre + "'");
        }
        Categoria categoria = new Categoria(nombre);
        if (request.activa() != null) {
            categoria.setActiva(request.activa());
        }
        return aRespuesta(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscar(id);
        String nombre = request.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "Ya existe otra categoria con el nombre '" + nombre + "'");
        }
        categoria.setNombre(nombre);
        if (request.activa() != null) {
            categoria.setActiva(request.activa());
        }
        return aRespuesta(categoriaRepository.save(categoria));
    }

    /** Solo se elimina una categoria vacia; si tiene productos se debe desactivar o mover sus productos. */
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscar(id);
        if (productoRepository.existsByCategoriaId(id)) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "La categoria '" + categoria.getNombre()
                            + "' tiene productos asociados; desactivela o mueva sus productos antes de eliminarla");
        }
        categoriaRepository.delete(categoria);
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoria con id " + id));
    }

    private CategoriaResponse aRespuesta(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNombre(), categoria.isActiva());
    }
}
