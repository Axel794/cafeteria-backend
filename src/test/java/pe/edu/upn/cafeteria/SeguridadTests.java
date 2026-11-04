package pe.edu.upn.cafeteria;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.cafeteria.entity.Categoria;
import pe.edu.upn.cafeteria.entity.Producto;
import pe.edu.upn.cafeteria.entity.Rol;
import pe.edu.upn.cafeteria.repository.CategoriaRepository;
import pe.edu.upn.cafeteria.repository.ProductoRepository;
import pe.edu.upn.cafeteria.security.JwtService;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de seguridad: login, JWT, 401 sin token, 403 por rol incorrecto y CORS para Angular. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SeguridadTests extends ApiTestBase {

    private static final String ORIGEN_ANGULAR = "http://localhost:4200";

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Value("${app.jwt.secret}")
    private String secretoJwt;

    private Categoria categoria;
    private Producto producto;

    @BeforeEach
    void preparar() {
        categoria = categoriaRepository.save(new Categoria("Cafes"));
        Producto nuevo = new Producto();
        nuevo.setNombre("Espresso");
        nuevo.setPrecio(new BigDecimal("8.00"));
        nuevo.setCategoria(categoria);
        producto = productoRepository.save(nuevo);
    }

    // ---------- Login ----------

    @Test
    void loginDelAdministradorDevuelveTokenYRol() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin_test\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void loginDelBaristaDevuelveSuRol() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"barista_test\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("BARISTA"));
    }

    @Test
    void loginConContrasenaIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin_test\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contrasena incorrectos"));
    }

    @Test
    void loginConUsuarioInexistenteDevuelve401ConElMismoMensaje() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"fantasma\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contrasena incorrectos"));
    }

    @Test
    void loginSinDatosDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isNotEmpty());
    }

    @Test
    void laContrasenaSeGuardaCifradaConBcrypt() {
        String hash = usuarioRepository.findByUsername("admin_test").orElseThrow().getPasswordHash();
        org.junit.jupiter.api.Assertions.assertNotEquals(CLAVE, hash);
        org.junit.jupiter.api.Assertions.assertTrue(hash.startsWith("$2"));
    }

    // ---------- Rutas publicas ----------

    @Test
    void laCartaYElTrackerSonPublicos() throws Exception {
        mockMvc.perform(get("/api/v1/categorias")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/productos")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/productos/" + producto.getId())).andExpect(status().isOk());
    }

    @Test
    void registrarUnPedidoNoRequiereToken() throws Exception {
        String json = """
                {"tipoPedido":"LOCAL","numeroMesa":1,
                 "detalles":[{"productoId":%d,"cantidad":1}]}
                """.formatted(producto.getId());

        mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    // ---------- 401: sin token o token invalido ----------

    @Test
    void escribirEnElCatalogoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nueva\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo\",\"precio\":5,\"categoriaId\":" + categoria.getId() + "}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/productos/" + producto.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"precio\":5,\"categoriaId\":" + categoria.getId() + "}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/productos/" + producto.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/categorias/" + categoria.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lasRutasDelPanelDelBaristaSinTokenDevuelven401() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/pedidos/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/pedidos/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_PREPARACION\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenInventadoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos")
                        .header(AUTHORIZATION, "Bearer esto.no.es.un.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenFirmadoConOtraClaveDevuelve401() throws Exception {
        String ajeno = new JwtService("otra-clave-distinta-de-al-menos-32-bytes-0123456789", 60)
                .generarToken("admin_test", Rol.ADMINISTRADOR);

        mockMvc.perform(post("/api/v1/categorias")
                        .header(AUTHORIZATION, bearer(ajeno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nueva\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenExpiradoDevuelve401() throws Exception {
        String expirado = new JwtService(secretoJwt, -5)
                .generarToken("admin_test", Rol.ADMINISTRADOR);

        mockMvc.perform(get("/api/v1/pedidos")
                        .header(AUTHORIZATION, bearer(expirado)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenValidoPermiteElAcceso() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos")
                        .header(AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk());
    }

    // ---------- 403: autenticado pero sin el rol necesario ----------

    @Test
    void elBaristaNoPuedeModificarElCatalogoDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/categorias")
                        .header(AUTHORIZATION, bearer(tokenBarista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nueva\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(put("/api/v1/productos/" + producto.getId())
                        .header(AUTHORIZATION, bearer(tokenBarista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"precio\":5,\"categoriaId\":" + categoria.getId() + "}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/productos/" + producto.getId())
                        .header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(status().isForbidden());
    }

    @Test
    void elBaristaSiPuedeUsarElPanelDeComandas() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos")
                        .header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(status().isOk());
    }

    @Test
    void elAdministradorTambienPuedeUsarElPanelDeComandas() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos")
                        .header(AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk());
    }

    // ---------- CORS para el frontend Angular ----------

    @Test
    void corsPermiteElOrigenDeAngularEnLaPeticionPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/productos")
                        .header("Origin", ORIGEN_ANGULAR)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEN_ANGULAR))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("DELETE")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsStringIgnoringCase("authorization")));
    }

    @Test
    void corsRechazaUnOrigenNoPermitido() throws Exception {
        mockMvc.perform(options("/api/v1/productos")
                        .header("Origin", "http://sitio-no-autorizado.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void laRespuestaIncluyeLaCabeceraCorsParaElOrigenPermitido() throws Exception {
        mockMvc.perform(get("/api/v1/productos").header("Origin", ORIGEN_ANGULAR))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEN_ANGULAR));

        mockMvc.perform(get("/api/v1/productos").header("Origin", "http://sitio-no-autorizado.com"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
