package pe.edu.upn.cafeteria.dto;

import java.math.BigDecimal;

public record DetallePedidoResponse(
        Long productoId,
        String producto,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        String notas) {
}
