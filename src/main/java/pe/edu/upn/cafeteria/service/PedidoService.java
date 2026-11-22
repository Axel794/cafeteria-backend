package pe.edu.upn.cafeteria.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.cafeteria.dto.DetallePedidoRequest;
import pe.edu.upn.cafeteria.dto.DetallePedidoResponse;
import pe.edu.upn.cafeteria.dto.PaginaResponse;
import pe.edu.upn.cafeteria.dto.PedidoRequest;
import pe.edu.upn.cafeteria.dto.PedidoResumenResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class PedidoService {

    /** Tamanio de pagina por defecto del historial y maximo permitido (evita pedir todo en una sola respuesta). */
    public static final int TAMANIO_POR_DEFECTO = 10;
    public static final int TAMANIO_MAXIMO = 50;

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

    /** HU04: comandas para el panel del barista, las mas antiguas primero; filtro opcional por estado. */
    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(EstadoPedido estado) {
        List<Pedido> pedidos = (estado == null)
                ? pedidoRepository.findAllByOrderByFechaHoraAsc()
                : pedidoRepository.findByEstadoOrderByFechaHoraAsc(estado);
        return pedidos.stream().map(this::aRespuesta).toList();
    }

    /**
     * PC6: historial paginado, mas recientes primero. Filtros opcionales: estado, rango de fechas
     * (inclusive) y texto (nombre del cliente o ticket). Solo lectura.
     */
    @Transactional(readOnly = true)
    public PaginaResponse<PedidoResumenResponse> historial(EstadoPedido estado, LocalDate desde, LocalDate hasta,
                                                           String texto, int pagina, int tamanio) {
        if (pagina < 0) {
            throw new ReglaNegocioException(HttpStatus.BAD_REQUEST, "La pagina debe ser 0 o mayor");
        }
        if (tamanio < 1 || tamanio > TAMANIO_MAXIMO) {
            throw new ReglaNegocioException(HttpStatus.BAD_REQUEST,
                    "El tamanio de pagina debe estar entre 1 y " + TAMANIO_MAXIMO);
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ReglaNegocioException(HttpStatus.BAD_REQUEST,
                    "La fecha 'desde' no puede ser posterior a 'hasta'");
        }

        List<EstadoPedido> estados = (estado == null) ? Arrays.asList(EstadoPedido.values()) : List.of(estado);
        LocalDateTime inicio = (desde == null) ? LocalDateTime.of(1970, 1, 1, 0, 0) : desde.atStartOfDay();
        LocalDateTime fin = (hasta == null) ? LocalDateTime.of(9999, 12, 31, 0, 0) : hasta.plusDays(1).atStartOfDay();
        String patron = "%" + (texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT)) + "%";

        return PaginaResponse.de(
                pedidoRepository.buscarHistorial(estados, inicio, fin, patron, PageRequest.of(pagina, tamanio)));
    }

    /**
     * HU05 / RF08: el estado solo avanza un paso a la vez
     * (PENDIENTE -> EN_PREPARACION -> LISTO -> ENTREGADO); no se permiten saltos ni retrocesos.
     */
    @Transactional
    public PedidoResponse cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = buscar(id);
        EstadoPedido actual = pedido.getEstado();

        if (actual == EstadoPedido.ENTREGADO) {
            throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El pedido ya fue entregado y no admite mas cambios de estado");
        }
        EstadoPedido siguiente = EstadoPedido.values()[actual.ordinal() + 1];
        if (nuevoEstado != siguiente) {
            throw new ReglaNegocioException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No se puede pasar de " + actual + " a " + nuevoEstado
                            + "; el siguiente estado permitido es " + siguiente);
        }
        pedido.setEstado(siguiente);
        return aRespuesta(pedidoRepository.save(pedido));
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
