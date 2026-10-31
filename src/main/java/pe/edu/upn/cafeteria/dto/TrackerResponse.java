package pe.edu.upn.cafeteria.dto;

import pe.edu.upn.cafeteria.entity.EstadoPedido;
import pe.edu.upn.cafeteria.entity.TipoPedido;

import java.time.LocalDateTime;

/** Vista reducida para el Order Tracker del cliente. */
public record TrackerResponse(
        Long id,
        String codigoTicket,
        EstadoPedido estado,
        TipoPedido tipoPedido,
        LocalDateTime fechaHora,
        String mensaje) {
}
