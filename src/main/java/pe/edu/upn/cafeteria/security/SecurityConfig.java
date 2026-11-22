package pe.edu.upn.cafeteria.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * Seguridad de la API: sin sesiones (stateless), autenticacion con JWT y autorizacion por rol.
 *
 * <pre>
 * Publico : POST /auth/login, GET categorias y productos, POST pedidos, GET pedidos/{id}/tracker
 * Publico (documentacion) : /swagger-ui.html y /v3/api-docs
 * BARISTA o ADMINISTRADOR : GET pedidos (incluye /historial), GET pedidos/{id}, PUT pedidos/{id}/estado
 * ADMINISTRADOR : POST, PUT y DELETE de categorias y productos
 * </pre>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtService jwtService,
                                           ApiSecurityErrorHandler errorHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API sin sesion ni cookies: el token va en la cabecera
                .cors(Customizer.withDefaults()) // usa el bean corsConfigurationSource
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // para la consola H2
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(antMatcher(HttpMethod.OPTIONS, "/**")).permitAll()
                        .requestMatchers(antMatcher("/h2-console/**")).permitAll() // solo desarrollo
                        .requestMatchers(antMatcher("/error")).permitAll()
                        .requestMatchers(
                                antMatcher("/v3/api-docs/**"),
                                antMatcher("/swagger-ui/**"),
                                antMatcher("/swagger-ui.html")).permitAll() // documentacion OpenAPI (PC6)
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/v1/auth/login")).permitAll()
                        .requestMatchers(
                                antMatcher(HttpMethod.GET, "/api/v1/categorias/**"),
                                antMatcher(HttpMethod.GET, "/api/v1/productos/**")).permitAll()
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/v1/pedidos")).permitAll()
                        .requestMatchers(antMatcher(HttpMethod.GET, "/api/v1/pedidos/*/tracker")).permitAll()
                        .requestMatchers(antMatcher("/api/v1/pedidos/**")).hasAnyRole("ADMINISTRADOR", "BARISTA")
                        .requestMatchers(
                                antMatcher("/api/v1/categorias/**"),
                                antMatcher("/api/v1/productos/**")).hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion) throws Exception {
        return configuracion.getAuthenticationManager();
    }

    /** CORS: solo los origenes del frontend Angular (por defecto http://localhost:4200). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String[] origenesPermitidos) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(Arrays.asList(origenesPermitidos));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }
}
