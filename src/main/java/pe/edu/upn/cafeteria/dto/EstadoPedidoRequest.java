package pe.edu.upn.cafeteria.dto;

import jakarta.validation.constraints.NotNull;
import pe.edu.upn.cafeteria.entity.EstadoPedido;

public record EstadoPedidoRequest(
        @NotNull(message = "El estado es obligatorio (PENDIENTE, EN_PREPARACION, LISTO o ENTREGADO)")
        EstadoPedido estado) {
}
