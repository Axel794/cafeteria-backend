package pe.edu.upn.cafeteria.dto;

import java.math.BigDecimal;

public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        boolean disponible,
        Long categoriaId,
        String categoria,
        String imagenUrl,
        String alergenos) {
}
