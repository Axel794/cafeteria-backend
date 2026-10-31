package pe.edu.upn.cafeteria;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.cafeteria.entity.Categoria;
import pe.edu.upn.cafeteria.entity.Producto;
import pe.edu.upn.cafeteria.repository.CategoriaRepository;
import pe.edu.upn.cafeteria.repository.ProductoRepository;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion de los endpoints iniciales (GET y POST) contra H2 en memoria.
 * Cada prueba se revierte al terminar (@Transactional), asi que no se contaminan entre si.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CafeteriaApiTests {

    @Autowired
    private MockMvc mockMvc;

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

    // ---------- Categorias ----------

    @Test
    void listarCategoriasDevuelveSoloLasActivas() throws Exception {
        mockMvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Cafes"));
    }

    @Test
    void crearCategoriaDuplicadaDevuelve409() throws Exception {
        mockMvc.perform(post("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"cafes\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void crearCategoriaNuevaDevuelve201() throws Exception {
        mockMvc.perform(post("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Reposteria\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.activa").value(true));
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isNotEmpty());
    }

    @Test
    void crearProductoConCategoriaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Latte\",\"precio\":13.00,\"categoriaId\":999999}"))
                .andExpect(status().isNotFound());
    }

    // ---------- Pedidos ----------

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
    void trackerDevuelveElEstadoActualDelPedido() throws Exception {
        String json = """
                {"tipoPedido":"LOCAL","numeroMesa":7,
                 "detalles":[{"productoId":%d,"cantidad":1}]}
                """.formatted(cappuccino.getId());

        MvcResult creado = mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        Number id = JsonPath.read(creado.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/pedidos/" + id.longValue() + "/tracker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());

        mockMvc.perform(get("/api/v1/pedidos/" + id.longValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles", hasSize(1)));
    }

    @Test
    void consultarPedidoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos/999999"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/pedidos/999999/tracker"))
                .andExpect(status().isNotFound());
    }
}
