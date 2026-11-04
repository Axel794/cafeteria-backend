package pe.edu.upn.cafeteria;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pe.edu.upn.cafeteria.entity.Rol;
import pe.edu.upn.cafeteria.entity.Usuario;
import pe.edu.upn.cafeteria.repository.UsuarioRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Utilidades comunes: crea un administrador y un barista de prueba y obtiene sus tokens con el login real. */
abstract class ApiTestBase {

    protected static final String CLAVE = "Clave123*";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected String tokenAdmin;
    protected String tokenBarista;

    @BeforeEach
    void crearUsuariosYTokens() throws Exception {
        crearUsuario("admin_test", Rol.ADMINISTRADOR);
        crearUsuario("barista_test", Rol.BARISTA);
        tokenAdmin = login("admin_test", CLAVE);
        tokenBarista = login("barista_test", CLAVE);
    }

    private void crearUsuario(String username, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(CLAVE));
        usuario.setRol(rol);
        usuarioRepository.save(usuario);
    }

    protected String login(String username, String password) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}
