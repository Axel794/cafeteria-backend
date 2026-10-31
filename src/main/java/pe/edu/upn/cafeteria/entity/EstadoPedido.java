package pe.edu.upn.cafeteria.entity;

/** Flujo de estados de un pedido: PENDIENTE -> EN_PREPARACION -> LISTO -> ENTREGADO. */
public enum EstadoPedido {
    PENDIENTE,
    EN_PREPARACION,
    LISTO,
    ENTREGADO
}
