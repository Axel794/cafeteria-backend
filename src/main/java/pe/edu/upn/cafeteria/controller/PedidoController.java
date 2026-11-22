package pe.edu.upn.cafeteria.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upn.cafeteria.dto.EstadoPedidoRequest;
import pe.edu.upn.cafeteria.dto.PaginaResponse;
import pe.edu.upn.cafeteria.dto.PedidoRequest;
import pe.edu.upn.cafeteria.dto.PedidoResumenResponse;
import pe.edu.upn.cafeteria.dto.PedidoResponse;
import pe.edu.upn.cafeteria.dto.TrackerResponse;
import pe.edu.upn.cafeteria.entity.EstadoPedido;
import pe.edu.upn.cafeteria.service.PedidoService;

import java.time.LocalDate;
import java.util.List;

/**
 * Publico: POST (crear pedido) y GET /{id}/tracker. Con token de BARISTA o ADMINISTRADOR:
 * GET (listado), GET /{id} y PUT /{id}/estado (ver SecurityConfig).
 */
@Tag(name = "Pedidos")
@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @Operation(summary = "Crear pedido (publico): valida, calcula total y genera ticket. 201 / 400 / 404 / 422")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(@Valid @RequestBody PedidoRequest request) {
        return pedidoService.crear(request);
    }

    @Operation(summary = "Listar comandas sin paginar para el panel (BARISTA/ADMIN), filtro opcional por estado")
    @GetMapping
    public List<PedidoResponse> listar(@RequestParam(required = false) EstadoPedido estado) {
        return pedidoService.listar(estado);
    }

    @Operation(summary = "Historial paginado (BARISTA/ADMIN): page, size (1-50), estado, desde, hasta, q")
    /** PC6: historial paginado con filtros opcionales (GET /api/v1/pedidos/historial?page=0&size=10). */
    @GetMapping("/historial")
    public PaginaResponse<PedidoResumenResponse> historial(
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return pedidoService.historial(estado, desde, hasta, q, page, size);
    }

    @Operation(summary = "Obtener un pedido con su detalle (BARISTA/ADMIN)")
    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id) {
        return pedidoService.obtener(id);
    }

    @Operation(summary = "Avanzar el estado un paso (BARISTA/ADMIN). 422 si es un salto o retroceso")
    @PutMapping("/{id}/estado")
    public PedidoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoPedidoRequest request) {
        return pedidoService.cambiarEstado(id, request.estado());
    }

    @Operation(summary = "Seguimiento publico del pedido para el cliente")
    @GetMapping("/{id}/tracker")
    public TrackerResponse tracker(@PathVariable Long id) {
        return pedidoService.tracker(id);
    }
}
