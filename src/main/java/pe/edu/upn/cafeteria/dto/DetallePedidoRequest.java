package pe.edu.upn.cafeteria.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DetallePedidoRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad minima es 1")
        @Max(value = 20, message = "La cantidad maxima por producto es 20")
        Integer cantidad,

        @Size(max = 255, message = "Las notas no pueden superar 255 caracteres")
        String notas) {
}
