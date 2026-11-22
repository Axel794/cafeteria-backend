package pe.edu.upn.cafeteria;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.cafeteria.entity.Categoria;
import pe.edu.upn.cafeteria.entity.Producto;
import pe.edu.upn.cafeteria.repository.CategoriaRepository;
import pe.edu.upn.cafeteria.repository.ProductoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PC6: pruebas del historial paginado (GET /api/v1/pedidos/historial) y de la documentacion OpenAPI.
 * Se crean 12 pedidos (10 en local y 2 Pick-Up de "Ana" ... ver preparar) y cada prueba se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HistorialPaginadoTests extends ApiTestBase {

    private static final String URL = "/api/v1/pedidos/historial";

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    private Producto cappuccino;
    private long primerPedidoId;

    @BeforeEach
    void preparar() throws Exception {
        Categoria cafes = categoriaRepository.save(new Categoria("Cafes"));
        cappuccino = new Producto();
        cappuccino.setNombre("Cappuccino");
        cappuccino.setPrecio(new BigDecimal("12.50"));
        cappuccino.setDisponible(true);
        cappuccino.setCategoria(cafes);
        cappuccino = productoRepository.save(cappuccino);

        // 10 pedidos en local (mesas 1..10) + 2 Pick-Up a nombre de Ana = 12 pedidos
        for (int mesa = 1; mesa <= 10; mesa++) {
            crear("{\"tipoPedido\":\"LOCAL\",\"numeroMesa\":" + mesa + ",\"detalles\":[{\"productoId\":"
                    + cappuccino.getId() + ",\"cantidad\":1}]}");
        }
        crear("{\"tipoPedido\":\"PICKUP\",\"nombreCliente\":\"Ana Torres\",\"telefonoCliente\":\"999111222\","
                + "\"detalles\":[{\"productoId\":" + cappuccino.getId() + ",\"cantidad\":2}]}");
        crear("{\"tipoPedido\":\"PICKUP\",\"nombreCliente\":\"Ana Ruiz\",\"telefonoCliente\":\"999333444\","
                + "\"detalles\":[{\"productoId\":" + cappuccino.getId() + ",\"cantidad\":1}]}");
    }

    private void crear(String json) throws Exception {
        String respuesta = mockMvc.perform(post("/api/v1/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        if (primerPedidoId == 0) {
            primerPedidoId = ((Number) com.jayway.jsonpath.JsonPath.read(respuesta, "$.id")).longValue();
        }
    }

    @Test
    void primeraPaginaTraeElTamanioPedidoYLosTotales() throws Exception {
        mockMvc.perform(get(URL).param("page", "0").param("size", "5")
                        .header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(5)))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanio").value(5))
                .andExpect(jsonPath("$.totalElementos").value(12))
                .andExpect(jsonPath("$.totalPaginas").value(3))
                .andExpect(jsonPath("$.primera").value(true))
                .andExpect(jsonPath("$.ultima").value(false));
    }

    @Test
    void ultimaPaginaTraeElRestoYMarcaUltima() throws Exception {
        mockMvc.perform(get(URL).param("page", "2").param("size", "5")
                        .header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(2)))
                .andExpect(jsonPath("$.ultima").value(true));
    }

    @Test
    void paginaFueraDeRangoDevuelveListaVaciaSinError() throws Exception {
        mockMvc.perform(get(URL).param("page", "50").param("size", "5")
                        .header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(0)))
                .andExpect(jsonPath("$.totalElementos").value(12));
    }

    @Test
    void ordenaLosMasRecientesPrimero() throws Exception {
        // el ultimo pedido creado (Ana Ruiz) es el mas reciente; el primero (mesa 1) queda al final
        mockMvc.perform(get(URL).param("size", "12").header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].nombreCliente").value("Ana Ruiz"))
                .andExpect(jsonPath("$.contenido[11].id").value(primerPedidoId));
    }

    @Test
    void sinParametrosUsaPaginaCeroYTamanioDiez() throws Exception {
        mockMvc.perform(get(URL).header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(10)))
                .andExpect(jsonPath("$.tamanio").value(10))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void cadaFilaTraeLaCantidadDeLineasSinElDetalle() throws Exception {
        mockMvc.perform(get(URL).param("size", "1").header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].cantidadLineas").value(1))
                .andExpect(jsonPath("$.contenido[0].detalles").doesNotExist());
    }

    @Test
    void filtraPorEstado() throws Exception {
        mockMvc.perform(put("/api/v1/pedidos/" + primerPedidoId + "/estado")
                        .header(AUTHORIZATION, bearer(tokenBarista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_PREPARACION\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(URL).param("estado", "EN_PREPARACION").header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(jsonPath("$.totalElementos").value(1));
        mockMvc.perform(get(URL).param("estado", "PENDIENTE").header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(jsonPath("$.totalElementos").value(11));
    }

    @Test
    void buscaPorNombreSinImportarMayusculas() throws Exception {
        mockMvc.perform(get(URL).param("q", "ANA").header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2));
        mockMvc.perform(get(URL).param("q", "ruiz").header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void buscaPorCodigoDeTicket() throws Exception {
        mockMvc.perform(get(URL).param("q", "cdd-").header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(jsonPath("$.totalElementos").value(12));
    }

    @Test
    void textoConComillasSeTrataComoDatoYNoComoSql() throws Exception {
        mockMvc.perform(get(URL).param("q", "' OR '1'='1' --").header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void filtraPorRangoDeFechas() throws Exception {
        LocalDate hoy = LocalDate.now();
        mockMvc.perform(get(URL).param("desde", hoy.toString()).param("hasta", hoy.toString())
                        .header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(jsonPath("$.totalElementos").value(12));
        mockMvc.perform(get(URL).param("desde", hoy.plusDays(1).toString())
                        .header(AUTHORIZATION, bearer(tokenBarista)))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void parametrosInvalidosDevuelven400() throws Exception {
        String auth = bearer(tokenBarista);
        mockMvc.perform(get(URL).param("size", "0").header(AUTHORIZATION, auth)).andExpect(status().isBadRequest());
        mockMvc.perform(get(URL).param("size", "51").header(AUTHORIZATION, auth)).andExpect(status().isBadRequest());
        mockMvc.perform(get(URL).param("page", "-1").header(AUTHORIZATION, auth)).andExpect(status().isBadRequest());
        mockMvc.perform(get(URL).param("size", "abc").header(AUTHORIZATION, auth)).andExpect(status().isBadRequest());
        mockMvc.perform(get(URL).param("estado", "INVENTADO").header(AUTHORIZATION, auth))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL).param("desde", "ayer").header(AUTHORIZATION, auth))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL).param("desde", "2030-02-01").param("hasta", "2030-01-01")
                        .header(AUTHORIZATION, auth))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void laDocumentacionOpenApiEsPublicaEIncluyeElHistorial() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").exists())
                .andExpect(jsonPath("$.paths['/api/v1/pedidos/historial'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists());
    }
}
