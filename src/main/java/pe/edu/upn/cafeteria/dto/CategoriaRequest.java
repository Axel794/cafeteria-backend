package pe.edu.upn.cafeteria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos para crear o actualizar una categoria. "activa" es opcional (por defecto true al crear). */
public record CategoriaRequest(
        @NotBlank(message = "El nombre de la categoria es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        Boolean activa) {
}
