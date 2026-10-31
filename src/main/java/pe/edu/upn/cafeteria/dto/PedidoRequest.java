package pe.edu.upn.cafeteria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.upn.cafeteria.entity.TipoPedido;

import java.util.List;

public record PedidoRequest(
        @NotNull(message = "El tipo de pedido es obligatorio (LOCAL o PICKUP)")
        TipoPedido tipoPedido,

        Integer numeroMesa,

        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombreCliente,

        @Size(max = 20, message = "El telefono no puede superar 20 caracteres")
        String telefonoCliente,

        @NotEmpty(message = "El pedido debe tener al menos un producto")
        @Valid
        List<DetallePedidoRequest> detalles) {
}
