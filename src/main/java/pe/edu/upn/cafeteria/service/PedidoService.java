package pe.edu.upn.cafeteria.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.cafeteria.dto.DetallePedidoRequest;
import pe.edu.upn.cafeteria.dto.DetallePedidoResponse;
import pe.edu.upn.cafeteria.dto.PedidoRequest;
import pe.edu.upn.cafeteria.dto.PedidoResponse;
import pe.edu.upn.cafeteria.dto.TrackerResponse;
import pe.edu.upn.cafeteria.entity.DetallePedido;
import pe.edu.upn.cafeteria.entity.EstadoPedido;
import pe.edu.upn.cafeteria.entity.Pedido;
import pe.edu.upn.cafeteria.entity.Producto;
import pe.edu.upn.cafeteria.entity.TipoPedido;
import pe.edu.upn.cafeteria.exception.RecursoNoEncontradoException;
import pe.edu.upn.cafeteria.exception.ReglaNegocioException;
import pe.edu.upn.cafeteria.repository.PedidoRepository;
import pe.edu.upn.cafeteria.repository.ProductoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;

    public PedidoService(PedidoRepository pedidoRepository, ProductoRepository productoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
    }

    /**
     * RF03, RF04 y RF05: valida la modalidad, calcula subtotales y total en el servidor,
     * guarda el pedido con estado PENDIENTE y le asigna un numero de ticket unico.
     */
    @Transactional
    public PedidoResponse crear(PedidoRequest request) {
        validarModalidad(request);

        Pedido pedido = new Pedido();
        pedido.setFechaHora(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setTipoPedido(request.tipoPedido());
        pedido.setNumeroMesa(request.tipoPedido() == TipoPedido.LOCAL ? request.numeroMesa() : null);
        pedido.setNombreCliente(request.nombreCliente());
        pedido.setTelefonoCliente(request.telefonoCliente());

        BigDecimal total = BigDecimal.ZERO;
        for (DetallePedidoRequest item : request.detalles()) {
            Producto producto = productoRepository.findById(item.productoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe el producto con id " + item.productoId()));

            if (!producto.isDisponible()) {
                throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "El producto '" + producto.getNombre() + "' no esta disponible");
            }

            BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(item.cantidad()));

            DetallePedido detalle = new DetallePedido();
            detalle.setProducto(producto);
            detalle.setCantidad(item.cantidad());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(subtotal);
            detalle.setNotas(item.notas());
            pedido.agregarDetalle(detalle);

            total = total.add(subtotal);
        }
        pedido.setTotal(total);

        Pedido guardado = pedidoRepository.save(pedido);
        guardado.setCodigoTicket(String.format("CDD-%06d", guardado.getId()));

        return aRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    /** RF06: estado actual del pedido con un mensaje listo para mostrar al cliente. */
    @Transactional(readOnly = true)
    public TrackerResponse tracker(Long id) {
        Pedido pedido = buscar(id);
        return new TrackerResponse(
                pedido.getId(),
                pedido.getCodigoTicket(),
                pedido.getEstado(),
                pedido.getTipoPedido(),
                pedido.getFechaHora(),
                mensajeDeEstado(pedido));
    }

    private void validarModalidad(PedidoRequest request) {
        if (request.tipoPedido() == TipoPedido.LOCAL) {
            if (request.numeroMesa() == null || request.numeroMesa() <= 0) {
                throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Para consumo en local se requiere un numero de mesa valido");
            }
        } else {
            if (esVacio(request.nombreCliente()) || esVacio(request.telefonoCliente())) {
                throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Para pedidos Pick-Up se requieren el nombre y el telefono del cliente");
            }
        }
    }

    private boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private Pedido buscar(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido con id " + id));
    }

    private String mensajeDeEstado(Pedido pedido) {
        return switch (pedido.getEstado()) {
            case PENDIENTE -> "Recibimos tu pedido y esta en cola.";
            case EN_PREPARACION -> "Estamos preparando tu pedido.";
            case LISTO -> pedido.getTipoPedido() == TipoPedido.PICKUP
                    ? "Tu pedido esta listo. Recogelo en barra."
                    : "Tu pedido esta listo y va en camino a tu mesa.";
            case ENTREGADO -> "Pedido entregado. Buen provecho.";
        };
    }

    private PedidoResponse aRespuesta(Pedido pedido) {
        List<DetallePedidoResponse> detalles = pedido.getDetalles().stream()
                .map(d -> new DetallePedidoResponse(
                        d.getProducto().getId(),
                        d.getProducto().getNombre(),
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        d.getSubtotal(),
                        d.getNotas()))
                .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getCodigoTicket(),
                pedido.getFechaHora(),
                pedido.getEstado(),
                pedido.getTipoPedido(),
                pedido.getNumeroMesa(),
                pedido.getNombreCliente(),
                pedido.getTelefonoCliente(),
                pedido.getTotal(),
                detalles);
    }
}
