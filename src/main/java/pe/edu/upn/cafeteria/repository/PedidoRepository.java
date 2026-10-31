package pe.edu.upn.cafeteria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upn.cafeteria.entity.EstadoPedido;
import pe.edu.upn.cafeteria.entity.Pedido;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Base para el Dashboard KDS del Sprint 2: comandas por estado, las mas antiguas primero. */
    List<Pedido> findByEstadoOrderByFechaHoraAsc(EstadoPedido estado);
}
