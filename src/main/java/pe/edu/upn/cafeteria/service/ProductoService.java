package pe.edu.upn.cafeteria.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.cafeteria.dto.ProductoRequest;
import pe.edu.upn.cafeteria.dto.ProductoResponse;
import pe.edu.upn.cafeteria.entity.Categoria;
import pe.edu.upn.cafeteria.entity.Producto;
import pe.edu.upn.cafeteria.exception.RecursoNoEncontradoException;
import pe.edu.upn.cafeteria.exception.ReglaNegocioException;
import pe.edu.upn.cafeteria.repository.CategoriaRepository;
import pe.edu.upn.cafeteria.repository.DetallePedidoRepository;
import pe.edu.upn.cafeteria.repository.ProductoRepository;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final DetallePedidoRepository detallePedidoRepository;

    public ProductoService(ProductoRepository productoRepository,
                           CategoriaRepository categoriaRepository,
                           DetallePedidoRepository detallePedidoRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.detallePedidoRepository = detallePedidoRepository;
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
        Producto producto = new Producto();
        copiarDatos(producto, request);
        producto.setDisponible(request.disponible() == null || request.disponible());
        return aRespuesta(productoRepository.save(producto));
    }

    /** RF09: actualiza precio, descripcion, categoria y disponibilidad (deshabilitar un producto agotado). */
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscar(id);
        copiarDatos(producto, request);
        if (request.disponible() != null) {
            producto.setDisponible(request.disponible());
        }
        return aRespuesta(productoRepository.save(producto));
    }

    /**
     * Elimina un producto solo si nunca se vendio. Si ya aparece en pedidos se rechaza con 409
     * para no perder el historial; en ese caso se debe marcar como no disponible.
     */
    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscar(id);
        if (detallePedidoRepository.existsByProductoId(id)) {
            throw new ReglaNegocioException(HttpStatus.CONFLICT,
                    "El producto '" + producto.getNombre()
                            + "' ya tiene pedidos asociados; marquelo como no disponible en lugar de eliminarlo");
        }
        productoRepository.delete(producto);
    }

    private void copiarDatos(Producto producto, ProductoRequest request) {
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la categoria con id " + request.categoriaId()));

        producto.setNombre(request.nombre().trim());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setCategoria(categoria);
        producto.setImagenUrl(request.imagenUrl());
        producto.setAlergenos(request.alergenos());
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
