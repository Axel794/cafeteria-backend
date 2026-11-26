-- Datos de prueba para las 7 tablas (PostgreSQL y H2). Ejecutar UNA sola vez, despues del primer arranque.
-- El arranque ya carga: usuario (admin, barista), categoria (4), producto (8) y configuracion (1).
-- Este script agrega: opcion_personalizacion (4), pedido (3) y detalle_pedido (6).
-- Integridad referencial: cada fila busca su producto, pedido o usuario por clave natural (nombre,
-- codigo_ticket, username); los totales coinciden con la suma de los subtotales.

INSERT INTO opcion_personalizacion (nombre, precio_adicional, disponible, producto_id) VALUES
 ('Leche de almendras', 2.00, TRUE, (SELECT id FROM producto WHERE nombre = 'Cappuccino')),
 ('Shot extra',         3.00, TRUE, (SELECT id FROM producto WHERE nombre = 'Cappuccino')),
 ('Leche de avena',     2.00, TRUE, (SELECT id FROM producto WHERE nombre = 'Latte')),
 ('Sin hielo',          0.00, TRUE, (SELECT id FROM producto WHERE nombre = 'Cold Brew'));

INSERT INTO pedido (codigo_ticket, fecha_hora, estado, tipo_pedido, numero_mesa, nombre_cliente, telefono_cliente, total, usuario_id) VALUES
 ('CDD-900001', CURRENT_TIMESTAMP, 'ENTREGADO',      'LOCAL',  5,    NULL,          NULL,        33.50, (SELECT id FROM usuario WHERE username = 'barista')),
 ('CDD-900002', CURRENT_TIMESTAMP, 'EN_PREPARACION', 'PICKUP', NULL, 'Lucia Ramos', '987654321', 29.00, (SELECT id FROM usuario WHERE username = 'barista')),
 ('CDD-900003', CURRENT_TIMESTAMP, 'PENDIENTE',      'LOCAL',  2,    NULL,          NULL,        28.50, NULL);

INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad, precio_unitario, subtotal, notas) VALUES
 ((SELECT id FROM pedido WHERE codigo_ticket = 'CDD-900001'), (SELECT id FROM producto WHERE nombre = 'Cappuccino'),             2, 12.00, 24.00, 'Con leche de almendras'),
 ((SELECT id FROM pedido WHERE codigo_ticket = 'CDD-900001'), (SELECT id FROM producto WHERE nombre = 'Brownie Artesanal'),      1,  9.50,  9.50, NULL),
 ((SELECT id FROM pedido WHERE codigo_ticket = 'CDD-900002'), (SELECT id FROM producto WHERE nombre = 'Cold Brew'),              1, 14.00, 14.00, 'Sin hielo'),
 ((SELECT id FROM pedido WHERE codigo_ticket = 'CDD-900002'), (SELECT id FROM producto WHERE nombre = 'V60'),                    1, 15.00, 15.00, NULL),
 ((SELECT id FROM pedido WHERE codigo_ticket = 'CDD-900003'), (SELECT id FROM producto WHERE nombre = 'Espresso'),               2,  8.00, 16.00, 'Sin azucar'),
 ((SELECT id FROM pedido WHERE codigo_ticket = 'CDD-900003'), (SELECT id FROM producto WHERE nombre = 'Cheesecake de Maracuya'), 1, 12.50, 12.50, NULL);
