package pe.edu.upn.cafeteria.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.cafeteria.dto.ProductoRequest;
import pe.edu.upn.cafeteria.dto.ProductoResponse;
import pe.edu.upn.cafeteria.entity.Categoria;
import pe.edu.upn.cafeteria.entity.Producto;
import pe.edu.upn.cafeteria.exception.RecursoNoEncontradoException;
import pe.edu.upn.cafeteria.repository.CategoriaRepository;
import pe.edu.upn.cafeteria.repository.ProductoRepository;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(ProductoRepository productoRepository, CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    /**
     * Devuelve el catalogo. Cada producto incluye su bandera "disponible" para que el
     * frontend pueda mostrar los agotados (RF01). Con soloDisponibles=true se filtran.
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(Long categoriaId, boolean soloDisponibles) {
        List<Producto> productos = (categoriaId == null)
                ? productoRepository.findAllByOrderByNombreAsc()
                : productoRepository.findByCategoriaIdOrderByNombreAsc(categoriaId);

        return productos.stream()
                .filter(producto -> !soloDisponibles || producto.isDisponible())
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la categoria con id " + request.categoriaId()));

        Producto producto = new Producto();
        producto.setNombre(request.nombre().trim());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setCategoria(categoria);
        producto.setImagenUrl(request.imagenUrl());
        producto.setAlergenos(request.alergenos());
        producto.setDisponible(request.disponible() == null || request.disponible());

        return aRespuesta(productoRepository.save(producto));
    }

    private Producto buscar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto con id " + id));
    }

    private ProductoResponse aRespuesta(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.isDisponible(),
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre(),
                producto.getImagenUrl(),
                producto.getAlergenos());
    }
}
