-- Consultas SELECT para evidenciar los datos de cada una de las 7 tablas (PostgreSQL y H2).
SELECT * FROM categoria ORDER BY id;
SELECT id, nombre, precio, disponible, alergenos, categoria_id FROM producto ORDER BY id;
SELECT * FROM opcion_personalizacion ORDER BY id;
SELECT id, codigo_ticket, estado, tipo_pedido, numero_mesa, nombre_cliente, telefono_cliente, total, usuario_id FROM pedido ORDER BY id;
SELECT * FROM detalle_pedido ORDER BY id;
SELECT id, username, rol, SUBSTRING(password_hash, 1, 20) AS password_hash_inicio FROM usuario ORDER BY id;
SELECT * FROM configuracion;

-- Integridad referencial: cada pedido con sus lineas y productos (JOIN).
SELECT p.codigo_ticket, p.estado, d.cantidad, pr.nombre AS producto, d.precio_unitario, d.subtotal, p.total
FROM pedido p JOIN detalle_pedido d ON d.pedido_id = p.id JOIN producto pr ON pr.id = d.producto_id
ORDER BY p.id, d.id;

-- Verificacion: el total del pedido debe ser igual a la suma de sus subtotales (no debe devolver filas).
SELECT p.codigo_ticket, p.total, SUM(d.subtotal) AS suma
FROM pedido p JOIN detalle_pedido d ON d.pedido_id = p.id
GROUP BY p.codigo_ticket, p.total HAVING p.total <> SUM(d.subtotal);
