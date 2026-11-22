package pe.edu.upn.cafeteria;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pe.edu.upn.cafeteria.dto.DetallePedidoRequest;
import pe.edu.upn.cafeteria.dto.PedidoRequest;
import pe.edu.upn.cafeteria.entity.Categoria;
import pe.edu.upn.cafeteria.entity.Producto;
import pe.edu.upn.cafeteria.entity.TipoPedido;
import pe.edu.upn.cafeteria.repository.CategoriaRepository;
import pe.edu.upn.cafeteria.repository.DetallePedidoRepository;
import pe.edu.upn.cafeteria.repository.PedidoRepository;
import pe.edu.upn.cafeteria.repository.ProductoRepository;
import pe.edu.upn.cafeteria.service.PedidoService;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PC6: verifica que PedidoService.crear (pedido + detalles + ticket) es atomico.
 *
 * Falla controlada: la 2.a linea trae notas de 300 caracteres (la columna admite 255). Se llama al servicio
 * directamente para saltar la validacion del DTO y que el error llegue a la base de datos DESPUES de haber
 * insertado el pedido y la 1.a linea. Si @Transactional funciona, todo se revierte y los conteos no cambian.
 *
 * Esta clase NO es @Transactional (si lo fuera, el rollback del servicio quedaria oculto dentro de la
 * transaccion de la prueba), por eso limpia sus propios datos en @AfterEach.
 */
@SpringBootTest
@ActiveProfiles("test")
class RollbackPedidoTests {

    @Autowired
    private PedidoService pedidoService;
    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private DetallePedidoRepository detallePedidoRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;

    private Categoria categoria;
    private Producto producto;

    @BeforeEach
    void preparar() {
        categoria = categoriaRepository.save(new Categoria("Categoria rollback"));
        Producto nuevo = new Producto();
        nuevo.setNombre("Producto rollback");
        nuevo.setPrecio(new BigDecimal("10.00"));
        nuevo.setDisponible(true);
        nuevo.setCategoria(categoria);
        producto = productoRepository.save(nuevo);
    }

    @AfterEach
    void limpiar() {
        pedidoRepository.deleteAll();
        productoRepository.delete(producto);
        categoriaRepository.delete(categoria);
    }

    private PedidoRequest pedidoCon(String notasDeLaSegundaLinea) {
        return new PedidoRequest(TipoPedido.LOCAL, 3, null, null, List.of(
                new DetallePedidoRequest(producto.getId(), 1, "sin azucar"),
                new DetallePedidoRequest(producto.getId(), 2, notasDeLaSegundaLinea)));
    }

    @Test
    void controlSinFallaGuardaUnPedidoConDosLineas() {
        long pedidosAntes = pedidoRepository.count();
        long lineasAntes = detallePedidoRepository.count();

        pedidoService.crear(pedidoCon("notas normales"));

        assertEquals(pedidosAntes + 1, pedidoRepository.count());
        assertEquals(lineasAntes + 2, detallePedidoRepository.count());
    }

    @Test
    void siFallaAlGuardarLasLineasSeRevierteTodoElPedido() {
        long pedidosAntes = pedidoRepository.count();
        long lineasAntes = detallePedidoRepository.count();

        assertThrows(RuntimeException.class, () -> pedidoService.crear(pedidoCon("x".repeat(300))));

        assertEquals(pedidosAntes, pedidoRepository.count(), "el pedido no debe quedar guardado");
        assertEquals(lineasAntes, detallePedidoRepository.count(), "ninguna linea debe quedar guardada");
    }
}
