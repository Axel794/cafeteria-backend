package pe.edu.upn.cafeteria.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.cafeteria.dto.CategoriaRequest;
import pe.edu.upn.cafeteria.dto.CategoriaResponse;
import pe.edu.upn.cafeteria.entity.Categoria;
import pe.edu.upn.cafeteria.exception.ReglaNegocioException;
import pe.edu.upn.cafeteria.repository.CategoriaRepository;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarActivas() {
        return categoriaRepository.findByActivaTrueOrderByNombreAsc().stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        String nombre = request.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "Ya existe una categoria con el nombre '" + nombre + "'");
        }
        Categoria guardada = categoriaRepository.save(new Categoria(nombre));
        return aRespuesta(guardada);
    }

    private CategoriaResponse aRespuesta(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNombre(), categoria.isActiva());
    }
}
