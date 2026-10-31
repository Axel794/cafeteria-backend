package pe.edu.upn.cafeteria.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductoRequest(
        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
        BigDecimal precio,

        @NotNull(message = "La categoria es obligatoria")
        Long categoriaId,

        @Size(max = 255, message = "La URL de imagen no puede superar 255 caracteres")
        String imagenUrl,

        @Size(max = 255, message = "Los alergenos no pueden superar 255 caracteres")
        String alergenos,

        Boolean disponible) {
}
