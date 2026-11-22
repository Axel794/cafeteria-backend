package pe.edu.upn.cafeteria.dto;

import pe.edu.upn.cafeteria.entity.EstadoPedido;
import pe.edu.upn.cafeteria.entity.TipoPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila liviana del historial: sin el detalle de productos, solo cuantas lineas tiene el pedido. */
public record PedidoResumenResponse(
        Long id,
        String codigoTicket,
        LocalDateTime fechaHora,
        EstadoPedido estado,
        TipoPedido tipoPedido,
        Integer numeroMesa,
        String nombreCliente,
        BigDecimal total,
        Long cantidadLineas) {
}
