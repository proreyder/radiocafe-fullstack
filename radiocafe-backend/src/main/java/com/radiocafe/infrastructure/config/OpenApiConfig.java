package com.radiocafe.infrastructure.config;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.context.annotation.*;
import java.util.List;
@Configuration
public class OpenApiConfig {
    @Bean OpenAPI radioCafeApi() {
        return new OpenAPI().info(new Info().title("RadioCafé API").version("1.1.0")
            .description("API académica con arquitectura hexagonal: catálogo, inventario, pedidos, usuarios y concurso Kappu. "
                + "Inicia sesión mediante POST /api/v1/auth/login. El explorador conserva la cookie y el selector de ventana, y obtiene automáticamente un token CSRF antes de cada escritura. "
                + "Los roles y la propiedad de los recursos se verifican en el servidor. Errores: 400 validación, 401 sesión, 403 permisos, 404 recurso, 409 regla de negocio o concurrencia. "
                + "La creación de pedidos reserva stock; su cancelación lo devuelve solo una vez. Los precios y recargos siempre se calculan en el backend."))
            .servers(List.of(new Server().url("/").description("Servidor actual")))
            .components(new Components().addSecuritySchemes("sesion",new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("JSESSIONID")
                .description("Cookie HttpOnly obtenida al iniciar sesión; el navegador la envía automáticamente, no se pega manualmente."))
                .addSecuritySchemes("ventana",new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name(TabSecurityContextRepository.HEADER)
                .description("UUID de esta ventana; mantiene identidades separadas dentro de la misma cookie. No autentica sin la cookie y un login previo. El explorador lo añade automáticamente."))
                .addSecuritySchemes("csrf",new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-CSRF-TOKEN")
                .description("Obtener token mediante GET /api/v1/auth/csrf antes de escribir; el explorador lo hace automáticamente.")))
            .addSecurityItem(new SecurityRequirement().addList("sesion"))
            .path("/api/v1/auth/logout",new PathItem().post(new Operation().tags(List.of("Autenticación")).summary("Cerrar sesión")
                .addSecurityItem(new SecurityRequirement().addList("sesion").addList("csrf"))
                .responses(new ApiResponses().addApiResponse("204",new ApiResponse().description("Sesión invalidada")))));
    }
}
