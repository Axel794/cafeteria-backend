package pe.edu.upn.cafeteria.dto;

import pe.edu.upn.cafeteria.entity.EstadoPedido;
import pe.edu.upn.cafeteria.entity.TipoPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        String codigoTicket,
        LocalDateTime fechaHora,
        EstadoPedido estado,
        TipoPedido tipoPedido,
        Integer numeroMesa,
        String nombreCliente,
        String telefonoCliente,
        BigDecimal total,
        List<DetallePedidoResponse> detalles) {
}
