package pe.edu.upn.cafeteria.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadatos de la documentacion OpenAPI y esquema "Bearer JWT" para probar endpoints protegidos desde Swagger UI. */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI cafeteriaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Cafeteria \"Como dice el dicho\"")
                        .version("v1")
                        .description("Menu digital y pedidos. Para los endpoints protegidos: POST /api/v1/auth/login, "
                                + "copiar el token y pulsar 'Authorize' (solo el token, sin 'Bearer')."))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
