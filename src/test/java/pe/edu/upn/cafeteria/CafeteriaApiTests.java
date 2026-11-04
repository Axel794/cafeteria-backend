package pe.edu.upn.cafeteria;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.cafeteria.entity.Categoria;
import pe.edu.upn.cafeteria.entity.Producto;
import pe.edu.upn.cafeteria.repository.CategoriaRepository;
import pe.edu.upn.cafeteria.repository.ProductoRepository;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion del CRUD completo (categorias, productos y pedidos) contra H2 en memoria.
 * Las operaciones de escritura se hacen con el token del rol que corresponde.
 * Cada prueba se revierte al terminar (@Transactional), asi que no se contaminan entre si.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CafeteriaApiTests extends ApiTestBase {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    private Categoria cafes;
    private Producto cappuccino;
    private Producto frappeAgotado;

    @BeforeEach
    void preparar() {
        cafes = categoriaRepository.save(new Categoria("Cafes"));

        Categoria antigua = new Categoria("Antigua");
        antigua.setActiva(false);
        categoriaRepository.save(antigua);

        cappuccino = guardarProducto("Cappuccino", "12.50", true);
        frappeAgotado = guardarProducto("Frappe", "16.00", false);
    }

    private Producto guardarProducto(String nombre, String precio, boolean disponible) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setPrecio(new BigDecimal(precio));
        producto.setDisponible(disponible);
        producto.setCategoria(cafes);
        return productoRepository.save(producto);
    }

    /** Crea un pedido LOCAL de 1 cappuccino (publico, sin token) y devuelve su id. */
    private long crearPedidoLocal() throws Exception {
        String json = """
                {"tipoPedido":"LOCAL","numeroMesa":4,
                 "detalles":[{"productoId":%d,"cantidad":1}]}
                """.formatted(cappuccino.getId());

        MvcResult creado = mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        Number id = JsonPath.read(creado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private void cambiarEstado(long pedidoId, String estado, int esperado) throws Exception {
        mockMvc.perform(put("/api/v1/pedidos/" + pedidoId + "/estado")
                        .header(AUTHORIZATION, bearer(tokenBarista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"" + estado + "\"}"))
                .andExpect(status().is(esperado));
    }

    // ---------- Categorias ----------

    @Test
    void listarCategoriasDevuelveSoloLasActivas() throws Exception {
        mockMvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Cafes"));
    }

    @Test
    void obtenerCategoriaPorIdDevuelve200() throws Exception {
        mockMvc.perform(get("/api/v1/categorias/" + cafes.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Cafes"));
    }

    @Test
    void obtenerCategoriaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/v1/categorias/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearCategoriaNuevaDevuelve201() throws Exception {
        mockMvc.perform(post("/api/v1/categorias")
                        .header(AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Reposteria\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    void crearCategoriaDuplicadaDevuelve409() throws Exception {
        mockMvc.perform(post("/api/v1/categorias")
                        .header(AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"cafes\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void crearCategoriaSinNombreDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/categorias")
                        .header(AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isNotEmpty());
    }

    @Test
    void actualizarCategoriaDevuelve200ConElNuevoNombre() throws Exception {
        mockMvc.perform(put("/api/v1/categorias/" + cafes.getId())
                        .header(AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Cafes de Origen\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Cafes de Origen"));
    }

    @Test
    void actualizarCategoriaConNombreDeOtraDevuelve409() throws Exception {
        mockMvc.perform(put("/api/v1/categorias/" + cafes.getId())
                        .header(AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Antigua\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void actualizarCategoriaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(put("/api/v1/categorias/999999")
                        .header(AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nueva\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarCategoriaConProductosDevuelve409() throws Exception {
        mockMvc.perform(delete("/api/v1/categorias/" + cafes.getId())
                        .header(AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminarCategoriaVaciaDevuelve204YYaNoExiste() throws Exception {
        Categoria vacia = categoriaRepository.save(new Categoria("Temporal"));

        mockMvc.perform(delete("/api/v1/categorias/" + vacia.getId())
                        .header(AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/categorias/" + vacia.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarCategoriaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(delete("/api/v1/categorias/999999")
                        .header(AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isNotFound());
    }

    // ---------- Productos ----------

    @Test
    void listarProductosPorCategoriaSoloDisponibles() throws Exception {
        mockMvc.perform(get("/api/v1/productos")
                        .param("categoriaId", String.valueOf(cafes.getId()))
                        .param("soloDisponibles", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Cappuccino"));
    }

    @Test
    void listarProductosIncluyeLosAgotadosConSuBandera() throws Exception {
        mockMvc.perform(get("/api/v1/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void obtenerProductoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/v1/productos/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearProductoValidoDevuelve201() throws Exception {
        String json = """
                {"nombre":"Latte","descripcion":"Espresso con leche","precio":13.00,"categoriaId":%d}
                """.formatted(cafes.getId());

        mockMvc.perform(post("/api/v1/productos")
                        .header(AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.categoria").value("Cafes"));
    }

    @Test
    void crearProductoConPrecioCeroDevuelve400() throws Exception {
        String json = """
                {"nombre":"Latte","precio":0,"categoriaId":%d}
                """.formatted(cafes.getId());

        mockMvc.perform(post("/api/v1/productos")
                        .header(AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isNotEmpty());
    }

    @Test
    void crearProductoConCategoriaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/api/v1/productos")
                        .header(AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Latte\",\"precio\":13.00,\"categoriaId\":999999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarProductoCambiaPrecioYDisponibilidad() throws Exception {
        String json = """
                {"nombre":"Cappuccino Doble","descripcion":"Doble shot","precio":15.00,
                 "categoriaId":%d,"disponible":false}
                """.formatted(cafes.getId());

        mockMvc.perform(put("/api/v1/productos/" + cappuccino.getId())
                        .header(AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Cappuccino Doble"))
                .andExpect(jsonPath("$.precio").value(15.0))
                .andExpect(jsonPath("$.disponible").value(false));
    }

    @Test
    void actualizarProductoConPrecioNegativoDevuelve400() throws Exception {
        String json = """
                {"nombre":"Cappuccino","precio":-5,"categoriaId":%d}
                """.formatted(cafes.getId());

        mockMvc.perform(put("/api/v1/productos/" + cappuccino.getId())
                        .header(AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarProductoInexistenteDevuelve404() throws Exception {
        String json = """
                {"nombre":"Fantasma","precio":10.00,"categoriaId":%d}
                """.formatted(cafes.getId());

        mockMvc.perform(put("/api/v1/productos/999999")
                        .header(AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarProductoConCategoriaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(put("/api/v1/productos/" + cappuccino.getId())
                        .header(AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Cappuccino\",\"precio\":12.00,\"categoriaId\":999999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarProductoSinPedidosDevuelve204YYaNoExiste() throws Exception {
        mockMvc.perform(delete("/api/v1/productos/" + frappeAgotado.getId())
                        .header(AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/productos/" + frappeAgotado.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarProductoConPedidosDevuelve409() throws Exception {
        crearPedidoLocal();

        mockMvc.perform(delete("/api/v1/productos/" + cappuccino.getId())
                        .header(AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void eliminarProductoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(delete("/api/v1/productos/999999")
                        .header(AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isNotFound());
    }

    // ---------- Pedidos: creacion y consulta ----------

    @Test
    void crearPedidoLocalDevuelve201PendienteConTotalCalculado() throws Exception {
        String json = """
                {"tipoPedido":"LOCAL","numeroMesa":5,
                 "detalles":[{"productoId":%d,"cantidad":2,"notas":"leche de almendra"}]}
                """.formatted(cappuccino.getId());

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.codigoTicket").value(startsWith("CDD-")))
                .andExpect(jsonPath("$.numeroMesa").value(5))
                .andExpect(jsonPath("$.total").value(25.0))
                .andExpect(jsonPath("$.detalles", hasSize(1)))
                .andExpect(jsonPath("$.detalles[0].subtotal").value(25.0));
    }

    @Test
    void crearPedidoPickupConDatosDeContactoDevuelve201() throws Exception {
        String json = """
                {"tipoPedido":"PICKUP","nombreCliente":"Ana","telefonoCliente":"999111222",
                 "detalles":[{"productoId":%d,"cantidad":1}]}
                """.formatted(cappuccino.getId());

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoPedido").value("PICKUP"))
                .andExpect(jsonPath("$.numeroMesa").doesNotExist());
    }

    @Test
    void pedidoLocalSinMesaDevuelve422() throws Exception {
        String json = """
                {"tipoPedido":"LOCAL","detalles":[{"productoId":%d,"cantidad":1}]}
                """.formatted(cappuccino.getId());

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void pedidoPickupSinTelefonoDevuelve422() throws Exception {
        String json = """
                {"tipoPedido":"PICKUP","nombreCliente":"Ana",
                 "detalles":[{"productoId":%d,"cantidad":1}]}
                """.formatted(cappuccino.getId());

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void pedidoConProductoAgotadoDevuelve422() throws Exception {
        String json = """
                {"tipoPedido":"LOCAL","numeroMesa":3,
                 "detalles":[{"productoId":%d,"cantidad":1}]}
                """.formatted(frappeAgotado.getId());

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void pedidoConProductoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoPedido\":\"LOCAL\",\"numeroMesa\":2,"
                                + "\"detalles\":[{\"productoId\":999999,\"cantidad\":1}]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void pedidoSinProductosDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoPedido\":\"LOCAL\",\"numeroMesa\":2,\"detalles\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pedidoConTipoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoPedido\":\"DELIVERY\",\"detalles\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pedidoConCantidadCeroDevuelve400() throws Exception {
        String json = """
                {"tipoPedido":"LOCAL","numeroMesa":2,
                 "detalles":[{"productoId":%d,"cantidad":0}]}
                """.formatted(cappuccino.getId());

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void trackerEsPublicoYElDetalleRequiereToken() throws Exception {
        long id = crearPedidoLocal();

        mockMvc.perform(get("/api/v1/pedidos/" + id + "/tracker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());

        mockMvc.perform(get("/api/v1/pedidos/" + id)
                        .header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles", hasSize(1)));
    }

    @Test
    void consultarPedidoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos/999999")
                        .header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/pedidos/999999/tracker"))
                .andExpect(status().isNotFound());
    }

    // ---------- Pedidos: listado para el panel del barista y cambio de estado ----------

    @Test
    void listarPedidosFiltraPorEstadoYOrdenaLosMasAntiguosPrimero() throws Exception {
        long primero = crearPedidoLocal();
        long segundo = crearPedidoLocal();
        cambiarEstado(primero, "EN_PREPARACION", 200);

        mockMvc.perform(get("/api/v1/pedidos")
                        .header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/v1/pedidos")
                        .param("estado", "PENDIENTE")
                        .header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value((int) segundo));
    }

    @Test
    void listarPedidosConEstadoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos")
                        .param("estado", "CANCELADO")
                        .header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elEstadoAvanzaPasoAPasoHastaEntregado() throws Exception {
        long id = crearPedidoLocal();

        cambiarEstado(id, "EN_PREPARACION", 200);
        cambiarEstado(id, "LISTO", 200);
        cambiarEstado(id, "ENTREGADO", 200);

        mockMvc.perform(get("/api/v1/pedidos/" + id + "/tracker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENTREGADO"));
    }

    @Test
    void noSePuedeSaltarUnEstadoDevuelve422() throws Exception {
        long id = crearPedidoLocal();
        cambiarEstado(id, "LISTO", 422);
    }

    @Test
    void noSePuedeRetrocederUnEstadoDevuelve422() throws Exception {
        long id = crearPedidoLocal();
        cambiarEstado(id, "EN_PREPARACION", 200);
        cambiarEstado(id, "PENDIENTE", 422);
    }

    @Test
    void unPedidoEntregadoNoAdmiteMasCambiosDevuelve422() throws Exception {
        long id = crearPedidoLocal();
        cambiarEstado(id, "EN_PREPARACION", 200);
        cambiarEstado(id, "LISTO", 200);
        cambiarEstado(id, "ENTREGADO", 200);
        cambiarEstado(id, "ENTREGADO", 422);
    }

    @Test
    void cambiarEstadoConValorInvalidoDevuelve400() throws Exception {
        long id = crearPedidoLocal();
        cambiarEstado(id, "CANCELADO", 400);
    }

    @Test
    void cambiarEstadoSinCuerpoValidoDevuelve400() throws Exception {
        long id = crearPedidoLocal();

        mockMvc.perform(put("/api/v1/pedidos/" + id + "/estado")
                        .header(AUTHORIZATION, bearer(tokenBarista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarEstadoDePedidoInexistenteDevuelve404() throws Exception {
        cambiarEstado(999999L, "EN_PREPARACION", 404);
    }
}
