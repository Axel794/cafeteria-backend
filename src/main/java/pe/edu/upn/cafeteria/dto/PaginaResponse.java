package pe.edu.upn.cafeteria.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Respuesta paginada estable (no se expone PageImpl de Spring, cuyo JSON no es contrato oficial). */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas,
        boolean primera,
        boolean ultima) {

    public static <T> PaginaResponse<T> de(Page<T> page) {
        return new PaginaResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
