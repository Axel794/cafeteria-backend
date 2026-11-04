package pe.edu.upn.cafeteria.controller;

import jakarta.validation.Valid;
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
import pe.edu.upn.cafeteria.dto.PedidoRequest;
import pe.edu.upn.cafeteria.dto.PedidoResponse;
import pe.edu.upn.cafeteria.dto.TrackerResponse;
import pe.edu.upn.cafeteria.entity.EstadoPedido;
import pe.edu.upn.cafeteria.service.PedidoService;

import java.util.List;

/**
 * Publico: POST (crear pedido) y GET /{id}/tracker. Con token de BARISTA o ADMINISTRADOR:
 * GET (listado), GET /{id} y PUT /{id}/estado (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(@Valid @RequestBody PedidoRequest request) {
        return pedidoService.crear(request);
    }

    @GetMapping
    public List<PedidoResponse> listar(@RequestParam(required = false) EstadoPedido estado) {
        return pedidoService.listar(estado);
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id) {
        return pedidoService.obtener(id);
    }

    @PutMapping("/{id}/estado")
    public PedidoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoPedidoRequest request) {
        return pedidoService.cambiarEstado(id, request.estado());
    }

    @GetMapping("/{id}/tracker")
    public TrackerResponse tracker(@PathVariable Long id) {
        return pedidoService.tracker(id);
    }
}
