package com.radiocafe.infrastructure.config;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.*;
import java.util.List;
@Configuration
public class CorsConfig {
 @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origins}") String origins) {
  var config=new CorsConfiguration();config.setAllowedOrigins(List.of(origins.split(",")));
  config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
  config.setAllowedHeaders(List.of("Content-Type","X-CSRF-TOKEN","X-Order-Token",TabSecurityContextRepository.HEADER));config.setAllowCredentials(true);
  var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/api/**",config);return source;
 }
}
