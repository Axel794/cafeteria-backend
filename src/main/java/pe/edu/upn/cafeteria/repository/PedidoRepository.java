package pe.edu.upn.cafeteria.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upn.cafeteria.dto.PedidoResumenResponse;
import pe.edu.upn.cafeteria.entity.EstadoPedido;
import pe.edu.upn.cafeteria.entity.Pedido;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Base para el Dashboard KDS del Sprint 2: comandas por estado, las mas antiguas primero. */
    List<Pedido> findByEstadoOrderByFechaHoraAsc(EstadoPedido estado);

    List<Pedido> findAllByOrderByFechaHoraAsc();

    /**
     * Historial paginado (PC6). JPQL con parametros nombrados: ningun valor del usuario se concatena
     * en la consulta, se enlazan como parametros. Devuelve un DTO liviano (proyeccion) y cuenta las lineas
     * con una subconsulta, asi una pagina cuesta 2 consultas (datos + total) sin importar su tamanio
     * (sin N+1 sobre los detalles). Orden fijo: lo mas reciente primero (fecha_hora DESC, id DESC).
     *
     * @param texto patron LIKE ya en minusculas (ej. "%ana%"); busca en nombre del cliente y en el ticket
     */
    @Query(value = """
            select new pe.edu.upn.cafeteria.dto.PedidoResumenResponse(
                p.id, p.codigoTicket, p.fechaHora, p.estado, p.tipoPedido,
                p.numeroMesa, p.nombreCliente, p.total,
                (select count(d) from DetallePedido d where d.pedido = p))
            from Pedido p
            where p.estado in :estados
              and p.fechaHora >= :desde
              and p.fechaHora < :hasta
              and (lower(coalesce(p.nombreCliente, '')) like :texto
                   or lower(coalesce(p.codigoTicket, '')) like :texto)
            order by p.fechaHora desc, p.id desc
            """,
            countQuery = """
            select count(p)
            from Pedido p
            where p.estado in :estados
              and p.fechaHora >= :desde
              and p.fechaHora < :hasta
              and (lower(coalesce(p.nombreCliente, '')) like :texto
                   or lower(coalesce(p.codigoTicket, '')) like :texto)
            """)
    Page<PedidoResumenResponse> buscarHistorial(@Param("estados") Collection<EstadoPedido> estados,
                                                @Param("desde") LocalDateTime desde,
                                                @Param("hasta") LocalDateTime hasta,
                                                @Param("texto") String texto,
                                                Pageable pageable);
}
